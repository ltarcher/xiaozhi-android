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
        try {
          _logger.i('___INFO Checking authorization status...');
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
            emit(OtaActivatedState());
            // 取消定时检查
            _cancelAuthorizationCheckTimer();
          } else {
            _logger.i('___INFO Device still not authorized');
            // 保持未授权状态，继续定时检查
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