package com.live2d;

import android.opengl.GLES20;

import com.live2d.sdk.cubism.framework.CubismDefaultParameterId;
import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.CubismModelSettingJson;
import com.live2d.sdk.cubism.framework.ICubismModelSetting;
import com.live2d.sdk.cubism.framework.effect.CubismBreath;
import com.live2d.sdk.cubism.framework.effect.CubismEyeBlink;
import com.live2d.sdk.cubism.framework.effect.CubismPose;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.id.CubismIdManager;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.model.CubismMoc;
import com.live2d.sdk.cubism.framework.model.CubismUserModel;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.CubismExpressionMotion;
import com.live2d.sdk.cubism.framework.motion.CubismMotion;
import com.live2d.sdk.cubism.framework.motion.IBeganMotionCallback;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;
import com.live2d.sdk.cubism.framework.physics.CubismPhysics;
import com.live2d.sdk.cubism.framework.rendering.CubismRenderer;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenSurfaceAndroid;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRendererAndroid;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live2D模型类
 * 封装单个Live2D模型的所有功能
 */
public class LAppModel extends CubismUserModel {
    // 模型所在目录
    protected String modelHomeDirectory;
    
    // 模型设置
    protected ICubismModelSetting modelSetting;
    
    // 眼睛闪烁ID列表
    protected final List<CubismId> eyeBlinkIds = new ArrayList<>();
    
    // 唇形同步ID列表
    protected final List<CubismId> lipSyncIds = new ArrayList<>();
    
    // 动作列表
    protected final Map<String, ACubismMotion> motions = new HashMap<>();
    
    // 表情列表
    protected final Map<String, ACubismMotion> expressions = new HashMap<>();
    
    // 是否启用随机眨眼
    protected boolean enableEyeBlink = true;
    
    // 是否启用唇形同步
    protected boolean enableLipSync = true;
    
    // 是否启动物理运算
    protected boolean enablePhysics = true;
    
    // 是否启用呼吸效果
    protected boolean enableBreath = true;
    
    // 用户时间（秒）
    private float userTimeSeconds = 0.0f;
    
    // 拖拽管理器
    private final TouchManager dragManager = TouchManager.getInstance();
    
    // X轴加速度
    protected float accelerationX = 0.0f;
    
    // Y轴加速度
    protected float accelerationY = 0.0f;
    
    // Z轴加速度
    protected float accelerationZ = 0.0f;
    
    // X轴拖拽値
    protected float dragX = 0.0f;
    
    // Y轴拖拽値
    protected float dragY = 0.0f;
    
    // 不透明度
    protected float opacity = 1.0f;
    
    // 参数ID：角度X
    private final CubismId idParamAngleX = CubismFramework.getIdManager().getId(CubismDefaultParameterId.ParameterId.ANGLE_X.getId());
    
    // 参数ID：角度Y
    private final CubismId idParamAngleY = CubismFramework.getIdManager().getId(CubismDefaultParameterId.ParameterId.ANGLE_Y.getId());
    
    // 参数ID：角度Z
    private final CubismId idParamAngleZ = CubismFramework.getIdManager().getId(CubismDefaultParameterId.ParameterId.ANGLE_Z.getId());
    
    // 参数ID：身体角度X
    private final CubismId idParamBodyAngleX = CubismFramework.getIdManager().getId(CubismDefaultParameterId.ParameterId.BODY_ANGLE_X.getId());
    
    // 参数ID：眼球X
    private final CubismId idParamEyeBallX = CubismFramework.getIdManager().getId(CubismDefaultParameterId.ParameterId.EYE_BALL_X.getId());
    
    // 参数ID：眼球Y
    private final CubismId idParamEyeBallY = CubismFramework.getIdManager().getId(CubismDefaultParameterId.ParameterId.EYE_BALL_Y.getId());
    
    // 唇形同步标志
    private boolean lipSync = false;
    
    /**
     * 构造函数
     */
    public LAppModel() {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppModel: Created");
        }
        
        // 初始化设置
        if (LAppDefine.AUTO_EYE_BLINK_ENABLE) {
            enableEyeBlink = true;
        }
        
        if (LAppDefine.AUTO_BREATH_ENABLE) {
            enableBreath = true;
        }
        
        if (LAppDefine.PHYSICS_ENABLE) {
            enablePhysics = true;
        }
        
        if (LAppDefine.LIP_SYNC_ENABLE) {
            enableLipSync = true;
        }
    }
    
    /**
     * 从assets目录加载模型资源
     * @param dir 模型目录
     * @param fileName 模型设置文件名
     */
    public void loadAssets(final String dir, final String fileName) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppModel: loadAssets 开始加载模型资源");
            LAppPal.printLog("LAppModel: 目录 = " + dir + ", 文件名 = " + fileName);
        }

        modelHomeDirectory = dir;
        String filePath = modelHomeDirectory + fileName;

        // 读取模型设置文件
        byte[] buffer = createBuffer(filePath);
        if (buffer == null || buffer.length == 0) {
            LAppPal.printErrorLog("LAppModel: 无法加载模型设置文件: " + filePath);
            return;
        }

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppModel: 模型设置文件加载成功，大小 = " + buffer.length + " 字节");
        }

        // 解析模型设置
        modelSetting = new CubismModelSettingJson(buffer);

        // 设置模型
        setupModel(modelSetting);
        if (model == null) {
            LAppPal.printErrorLog("LAppModel: 模型设置失败");
            return;
        }

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppModel: 模型设置完成");
        }

        // 设置渲染器
        CubismRenderer renderer = CubismRendererAndroid.create();
        setupRenderer(renderer);

        // 设置纹理
        setupTextures();

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppModel: loadAssets 完成");
        }
    }
    
    /**
     * 创建缓冲区（从文件读取数据）
     * @param filePath 文件路径
     * @return 文件内容字节数组
     */
    protected byte[] createBuffer(String filePath) {
        return LAppPal.loadFileAsBytes(filePath);
    }
    
    /**
     * 设置纹理
     */
    /**
     * 设置模型
     * @param setting 模型设置
     */
    private void setupModel(ICubismModelSetting setting) {
        modelSetting = setting;
        
        isUpdated = true;
        isInitialized = false;
        
        // 加载Cubism模型
        {
            String fileName = modelSetting.getModelFileName();
            if (!fileName.isEmpty()) {
                String path = modelHomeDirectory + fileName;
                
                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("create model: " + modelSetting.getModelFileName());
                }
                
                byte[] buffer = createBuffer(path);
                loadModel(buffer);
            }
        }
        
        // 加载表情文件(.exp3.json)
        {
            if (modelSetting.getExpressionCount() > 0) {
                final int count = modelSetting.getExpressionCount();
                
                for (int i = 0; i < count; i++) {
                    String name = modelSetting.getExpressionName(i);
                    String path = modelSetting.getExpressionFileName(i);
                    path = modelHomeDirectory + path;
                    
                    byte[] buffer = createBuffer(path);
                    CubismExpressionMotion motion = loadExpression(buffer);
                    
                    if (motion != null) {
                        expressions.put(name, motion);
                    }
                }
            }
        }
        
        // 物理效果
        {
            String path = modelSetting.getPhysicsFileName();
            if (!path.isEmpty()) {
                String modelPath = modelHomeDirectory + path;
                byte[] buffer = createBuffer(modelPath);
                
                loadPhysics(buffer);
            }
        }
        
        // 姿势
        {
            String path = modelSetting.getPoseFileName();
            if (!path.isEmpty()) {
                String modelPath = modelHomeDirectory + path;
                byte[] buffer = createBuffer(modelPath);
                loadPose(buffer);
            }
        }
        
        // 加载眼部闪烁数据
        if (modelSetting.getEyeBlinkParameterCount() > 0) {
            eyeBlink = CubismEyeBlink.create(modelSetting);
        }
        
        // 加载呼吸数据
        breath = CubismBreath.create();
        List<CubismBreath.BreathParameterData> breathParameters = new ArrayList<>();
        
        CubismIdManager idManager = CubismFramework.getIdManager();
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.ANGLE_X.getId()), 0.0f, 15.0f, 6.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.ANGLE_Y.getId()), 0.0f, 8.0f, 3.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.ANGLE_Z.getId()), 0.0f, 10.0f, 5.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.BODY_ANGLE_X.getId()), 0.0f, 4.0f, 15.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.BREATH.getId()), 0.5f, 0.5f, 3.2345f, 0.5f));
        
        breath.setParameters(breathParameters);
        
        // EyeBlinkIds
        int eyeBlinkIdCount = modelSetting.getEyeBlinkParameterCount();
        for (int i = 0; i < eyeBlinkIdCount; i++) {
            eyeBlinkIds.add(modelSetting.getEyeBlinkParameterId(i));
        }
        
        // LipSyncIds
        int lipSyncIdCount = modelSetting.getLipSyncParameterCount();
        for (int i = 0; i < lipSyncIdCount; i++) {
            lipSyncIds.add(modelSetting.getLipSyncParameterId(i));
        }
        
        if (modelSetting == null || modelMatrix == null) {
            LAppPal.printLog("Failed to setupModel().");
            return;
        }
        
        model.saveParameters();
        
        isUpdated = false;
        isInitialized = true;
    }
    
    /**
     * 设置纹理
     */
    private void setupTextures() {
        for (int modelTextureNumber = 0; modelTextureNumber < modelSetting.getTextureCount(); modelTextureNumber++) {
            // 纹理名为空字符时跳过加载和绑定处理
            if (modelSetting.getTextureFileName(modelTextureNumber).isEmpty()) {
                continue;
            }
            
            // 从PNGファイル创建OpenGL纹理
            String texturePath = modelSetting.getTextureFileName(modelTextureNumber);
            texturePath = modelHomeDirectory + texturePath;
            
            LAppTextureManager.TextureInfo texture =
                LAppDelegate.getInstance()
                            .getTextureManager()
                            .createTextureFromPngFile(texturePath);
            final int glTextureNumber = texture.id;
            
            this.<CubismRendererAndroid>getRenderer().bindTexture(modelTextureNumber, glTextureNumber);
            
            this.<CubismRendererAndroid>getRenderer().isPremultipliedAlpha(false);
        }
    }
    
    /**
     * 设置眼部闪烁
     */
    
    /**
     * 设置呼吸效果
     */
    protected void setupBreath() {
        if (!enableBreath) {
            return;
        }
        
        // 创建呼吸控制器
        breath = CubismBreath.create();
        
        // 获取呼吸参数列表
        List<CubismBreath.BreathParameterData> breathParameters = new ArrayList<>();
        
        // 添加呼吸参数
        CubismIdManager idManager = CubismFramework.getIdManager();
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.ANGLE_Z.getId()), 0.0f, 0.0f, 0.0f, 0.0f));
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.BODY_ANGLE_X.getId()), 0.0f, 0.0f, 0.0f, 0.0f));
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.BODY_ANGLE_Y.getId()), 0.0f, 0.0f, 0.0f, 0.0f));
        breathParameters.add(new CubismBreath.BreathParameterData(idManager.getId(CubismDefaultParameterId.ParameterId.BODY_ANGLE_Z.getId()), 0.0f, 0.0f, 0.0f, 0.0f));
        
        // 设置呼吸参数
        breath.setParameters(breathParameters);
    }
    
    /**
     * 设置物理效果
     */
    
    /**
     * 设置姿势
     */
    
    /**
     * 设置唇形同步参数ID
     */
    
    /**
     * 删除模型
     */
    public void deleteModel() {
        delete();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppModel: Model deleted");
        }
    }
    
    /**
     * 更新モデル
     */
    public void update() {
        final float deltaTimeSeconds = (float)LAppPal.getDeltaTime();
        userTimeSeconds += deltaTimeSeconds;
        
        // 更新拖拽参数
        dragX = dragManager.getCurrentX();
        dragY = dragManager.getCurrentY();
        
        // 模型パラメータ更新
        model.loadParameters();
        
        // 眼部闪烁
        if (enableEyeBlink && eyeBlink != null) {
            eyeBlink.updateParameters(model, deltaTimeSeconds);
        }
        
        // 表情
        if (expressionManager != null) {
            expressionManager.updateMotion(model, deltaTimeSeconds);
        }
        
        // 拖拽跟随機能
        model.addParameterValue(idParamAngleX, dragX * 30); // -30到30の値
        model.addParameterValue(idParamAngleY, dragY * 30);
        model.addParameterValue(idParamAngleZ, dragX * dragY * (-30));
        
        // 拖拽引起的身体方向調整
        model.addParameterValue(idParamBodyAngleX, dragX * 10); // -10到10の値
        
        // 拖拽引起的眼球方向調整
        model.addParameterValue(idParamEyeBallX, dragX);  // -1到1の値
        model.addParameterValue(idParamEyeBallY, dragY);
        
        // 呼吸效果
        if (enableBreath && breath != null) {
            breath.updateParameters(model, deltaTimeSeconds);
        }
        
        // 物理效果
        if (enablePhysics && physics != null) {
            physics.evaluate(model, deltaTimeSeconds);
        }
        
        // 唇形同期
        if (enableLipSync && lipSync) {
            // 実時唇形同期時、システムから音量を取得し0~1の範囲の値を入力
            float value = 0.0f;
            
            for (int i = 0; i < lipSyncIds.size(); i++) {
                CubismId lipSyncId = lipSyncIds.get(i);
                model.addParameterValue(lipSyncId, value, 0.8f);
            }
        }
        
        // 姿勢
        if (pose != null) {
            pose.updateParameters(model, deltaTimeSeconds);
        }
        
        // 更新モデルパラメータ
        model.update();
    }
    
    /**
     * 绘制模型
     * @param matrix 矩阵
     */
    public void draw(CubismMatrix44 matrix) {
        if (model == null) {
            return;
        }
        
        // 为了避免缓存変数の定義、multiplyではなくmultiplyByMatrixを使用
        CubismMatrix44.multiply(
            modelMatrix.getArray(),
            matrix.getArray(),
            matrix.getArray()
        );
        
        this.<CubismRendererAndroid>getRenderer().setMvpMatrix(matrix);
        this.<CubismRendererAndroid>getRenderer().drawModel();
    }
    
    /**
     * 碰撞检测テスト
     * 从指定IDの頂点リストから矩形を計算し、座標が矩形範囲内かを判定
     *
     * @param hitAreaName 碰撞検査の対象ID
     * @param x 判定するx座標
     * @param y 判定するy座標
     * @return 碰撞した場合はtrue
     */
    public boolean hitTest(final String hitAreaName, float x, float y) {
        // 透明な場合は当たり判定しない
        if (opacity < 1) {
            return false;
        }
        
        final int count = modelSetting.getHitAreasCount();
        for (int i = 0; i < count; i++) {
            if (modelSetting.getHitAreaName(i).equals(hitAreaName)) {
                final CubismId drawID = modelSetting.getHitAreaId(i);
                
                return isHit(drawID, x, y);
            }
        }
        // 存在しない場合はfalseを返す
        return false;
    }
    
    /**
     * 设置唇形同步値
     * @param value 唇形同期値
     */
    /**
     * 设置唇形同步値
     * @param value 唇形同期値
     */
    public void setLipSyncValue(float value) {
        if (!enableLipSync) {
            return;
        }
        
        for (CubismId id : lipSyncIds) {
            model.addParameterValue(id, value, 0.8f);
        }
    }
    
    /**
     * 设置加速度値
     * @param x X軸加速度
     * @param y Y軸加速度
     * @param z Z軸加速度
     */
    public void setAcceleration(float x, float y, float z) {
        accelerationX = x;
        accelerationY = y;
        accelerationZ = z;
    }
    
    /**
     * 获取模型设置
     * @return 模型設定オブジェクト
     */
    public ICubismModelSetting getModelSetting() {
        return modelSetting;
    }
    
    /**
     * 获取模型目录
     * @return 模型ディレクトリパス
     */
    public String getModelHomeDirectory() {
        return modelHomeDirectory;
    }
    
    /**
     * 启用/禁用眼睛闪烁
     * @param enable true启用，false禁用
     */
    public void setEyeBlinkEnabled(boolean enable) {
        enableEyeBlink = enable;
    }
    
    /**
     * 启用/禁用唇形同步
     * @param enable true启用，false禁用
     */
    public void setLipSyncEnabled(boolean enable) {
        enableLipSync = enable;
    }
    
    /**
     * 启用/禁用物理运算
     * @param enable true启用，false禁用
     */
    public void setPhysicsEnabled(boolean enable) {
        enablePhysics = enable;
    }
    
    /**
     * 启用/禁用呼吸效果
     * @param enable true启用，false禁用
     */
    public void setBreathEnabled(boolean enable) {
        enableBreath = enable;
    }
}