import 'dart:async';
import 'package:bloc/bloc.dart';
import 'package:dio/dio.dart';
import 'package:logger/logger.dart';
import 'package:meta/meta.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/util/shared_preferences_util.dart';

part 'ota_event.dart';
part 'ota_state.dart';

class OtaBloc extends Bloc<OtaEvent, OtaState> {
  final _dio = Dio();
  late Logger _logger;
  
  // 添加定时检查授权状态的定时器
  Timer? _authorizationCheckTimer;
  static const Duration _authorizationCheckInterval = Duration(seconds: 15); // 每15秒检查一次
  
  // 添加计数器，用于跟踪检查次数
  int _authorizationCheckCount = 0;

  OtaBloc() : super(OtaActivatedState()) {
    _logger = Logger();
    on<OtaEvent>((event, emit) async {
      if (event is OtaInitialEvent) {
        try {
          Response response = await _dio.post(
            await SharedPreferencesUtil().getOtaUrl() ?? XConst.defaultOtaUrl,
            data: {
              "mac_address": await SharedPreferencesUtil().getMacAddress(),
              "application": {"name": XConst.name},
            },
            options: Options(
              headers: {
                "Device-Id": await SharedPreferencesUtil().getMacAddress(),
              },
            ),
          );
          if (null == response.data['activation']) {
            emit(OtaActivatedState());
            // 在已授权状态下取消定时检查
            _cancelAuthorizationCheckTimer();
          } else {
            String? url;
            try {
              String domain =
                  (response.data['activation']['message'] as String)
                      .split('\n')
                      .first;
              String? otaUrl = await SharedPreferencesUtil().getOtaUrl();
              if (null != otaUrl && otaUrl.contains('https')) {
                url = 'https://$domain';
              } else {
                url = 'http://$domain';
              }
            } catch (_) {}
            emit(
              OtaNotActivatedState(
                code: response.data['activation']['code'],
                url: url,
              ),
            );
            // 在未授权状态下启动定时检查
            _startAuthorizationCheckTimer();
          }
        } catch (e, s) {
          _logger.e('___ERROR OTA $e $s');
        }
        
        @override
        Future<void> close() {
          _cancelAuthorizationCheckTimer();
          return super.close();
        }
      }
      
      if (event is OtaCheckAuthorizationEvent) {
        _authorizationCheckCount++;
        try {
          _logger.i('___INFO Checking authorization status... (attempt $_authorizationCheckCount)');
          Response response = await _dio.post(
            await SharedPreferencesUtil().getOtaUrl() ?? XConst.defaultOtaUrl,
            data: {
              "mac_address": await SharedPreferencesUtil().getMacAddress(),
              "application": {"name": XConst.name},
            },
            options: Options(
              headers: {
                "Device-Id": await SharedPreferencesUtil().getMacAddress(),
              },
            ),
          );
          if (null == response.data['activation']) {
            _logger.i('___INFO Device is now authorized');
            // 如果forceUpdate为true，或者当前状态不是已授权状态时，才发出新状态
            if (event.forceUpdate || state is! OtaActivatedState) {
              emit(OtaActivatedState());
            }
            // 取消定时检查
            _cancelAuthorizationCheckTimer();
          } else {
            _logger.i('___INFO Device still not authorized');
            // 获取授权信息
            String? url;
            try {
              String domain =
                  (response.data['activation']['message'] as String)
                      .split('\n')
                      .first;
              String? otaUrl = await SharedPreferencesUtil().getOtaUrl();
              if (null != otaUrl && otaUrl.contains('https')) {
                url = 'https://$domain';
              } else {
                url = 'http://$domain';
              }
            } catch (_) {}
            
            // 检查当前状态是否已经是相同的未授权状态
            bool shouldEmitState = true;
            if (state is OtaNotActivatedState) {
              final currentState = state as OtaNotActivatedState;
              // 如果授权码和URL都相同，则通常不需要更新状态
              if (currentState.code == response.data['activation']['code'] &&
                  currentState.url == url) {
                // 但是每3次检查（约45秒）强制触发一次状态更新，以确保授权窗口能定期显示
                if (_authorizationCheckCount % 3 != 0) {
                  _logger.i('___INFO Authorization state unchanged, skipping state update (check #$_authorizationCheckCount)');
                  shouldEmitState = false;
                } else {
                  _logger.i('___INFO Forcing state update to show authorization dialog (check #$_authorizationCheckCount)');
                }
              }
            }
            
            if (shouldEmitState) {
              emit(
                OtaNotActivatedState(
                  code: response.data['activation']['code'],
                  url: url,
                ),
              );
            }
          }
        } catch (e, s) {
          _logger.e('___ERROR OTA Check $e $s');
        }
      }
    });
  }
  
  // 启动授权状态定时检查
  void _startAuthorizationCheckTimer() {
    _cancelAuthorizationCheckTimer(); // 先取消之前的定时器
    _logger.i('___INFO Starting authorization check timer with interval: ${_authorizationCheckInterval.inSeconds} seconds');
    _authorizationCheckTimer = Timer.periodic(_authorizationCheckInterval, (timer) {
      // 只在网络连接正常的情况下检查授权状态
      if (_isNetworkAvailable()) {
        _logger.i('___INFO Triggering periodic authorization check');
        add(OtaCheckAuthorizationEvent());
      } else {
        _logger.i('___INFO Network not available, skipping authorization check');
      }
    });
  }
  
  // 检查网络是否可用
  bool _isNetworkAvailable() {
    // 这里可以添加更复杂的网络检查逻辑
    // 目前简单返回true，因为WebSocket连接存在通常意味着网络可用
    return true;
  }
  
  // 取消授权状态定时检查
  void _cancelAuthorizationCheckTimer() {
    if (_authorizationCheckTimer != null) {
      _authorizationCheckTimer!.cancel();
      _authorizationCheckTimer = null;
      _logger.i('___INFO Authorization check timer cancelled');
    }
  }
}