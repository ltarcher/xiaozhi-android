import 'package:shared_preferences/shared_preferences.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/util/common_utils.dart';

class SharedPreferencesUtil {
  static final SharedPreferencesUtil _instance =
      SharedPreferencesUtil._internal();

  factory SharedPreferencesUtil() {
    return _instance;
  }

  SharedPreferencesUtil._internal();

  final String _keyOtaUrl = 'OTA_URL';

  final String _keyWebsocketUrl = 'WEBSOCKET_URL';

  final String _keyMacAddress = 'MAC_ADDRESS';

  final String _keyLive2DGearVisible = 'LIVE2D_GEAR_VISIBLE';

  final String _keyLive2DPowerVisible = 'LIVE2D_POWER_VISIBLE';

  final String _keyWakeWord = 'WAKE_WORD';
  
  final String _keyLive2DModel = 'LIVE2D_MODEL';

  Future<void> init() async {
    String? otaUrl = await getOtaUrl();
    if (null == otaUrl) {
      await setOtaUrl(XConst.defaultOtaUrl);
    }

    String? websocketUrl = await getWebsocketUrl();
    if (null == websocketUrl) {
      await setWebsocketUrl(XConst.defaultWebsocketUrl);
    }

    String? macAddress = await getMacAddress();
    if (null == macAddress) {
      await setMacAddress(CommonUtils.generateUnicastMacAddress());
    }

    // 初始化Live2D按钮可见性默认值
    bool? gearVisible = await getLive2DGearVisible();
    if (gearVisible == null) {
      await setLive2DGearVisible(true); // 默认齿轮按钮可见
    }

    bool? powerVisible = await getLive2DPowerVisible();
    if (powerVisible == null) {
      await setLive2DPowerVisible(false); // 默认电源按钮不可见
    }
    
    // 初始化唤醒词默认值
    String? wakeWord = await getWakeWord();
    if (wakeWord == null || wakeWord.isEmpty) {
      await setWakeWord('你好，小清'); // 默认唤醒词
    }
    
    // 初始化Live2D模型默认值
    String? live2dModel = await getLive2DModel();
    if (live2dModel == null || live2dModel.isEmpty) {
      await setLive2DModel('Haru'); // 默认使用Haru模型
    }
  }

  Future<String?> getOtaUrl() async {
    return (await SharedPreferences.getInstance()).getString(_keyOtaUrl);
  }

  Future<bool> setOtaUrl(String value) async {
    return (await SharedPreferences.getInstance()).setString(_keyOtaUrl, value);
  }

  Future<String?> getWebsocketUrl() async {
    return (await SharedPreferences.getInstance()).getString(_keyWebsocketUrl);
  }

  Future<bool> setWebsocketUrl(String value) async {
    return (await SharedPreferences.getInstance()).setString(
      _keyWebsocketUrl,
      value,
    );
  }

  Future<String?> getMacAddress() async {
    return (await SharedPreferences.getInstance()).getString(_keyMacAddress);
  }

  Future<bool> setMacAddress(String value) async {
    return (await SharedPreferences.getInstance()).setString(
      _keyMacAddress,
      value,
    );
  }

  Future<bool?> getLive2DGearVisible() async {
    return (await SharedPreferences.getInstance()).getBool(_keyLive2DGearVisible);
  }

  Future<bool> setLive2DGearVisible(bool value) async {
    return (await SharedPreferences.getInstance()).setBool(_keyLive2DGearVisible, value);
  }

  Future<bool?> getLive2DPowerVisible() async {
    return (await SharedPreferences.getInstance()).getBool(_keyLive2DPowerVisible);
  }

  Future<bool> setLive2DPowerVisible(bool value) async {
    return (await SharedPreferences.getInstance()).setBool(_keyLive2DPowerVisible, value);
  }

  Future<String?> getWakeWord() async {
    return (await SharedPreferences.getInstance()).getString(_keyWakeWord);
  }

  Future<bool> setWakeWord(String value) async {
    return (await SharedPreferences.getInstance()).setString(_keyWakeWord, value);
  }

  Future<String?> getLive2DModel() async {
    return (await SharedPreferences.getInstance()).getString(_keyLive2DModel);
  }

  Future<bool> setLive2DModel(String value) async {
    return (await SharedPreferences.getInstance()).setString(_keyLive2DModel, value);
  }
}
