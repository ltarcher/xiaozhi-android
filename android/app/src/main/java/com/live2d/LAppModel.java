/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.util.Log;

import com.live2d.LAppDefine;
import com.live2d.sdk.cubism.framework.CubismDefaultParameterId.ParameterId;
import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.CubismModelSettingJson;
import com.live2d.sdk.cubism.framework.ICubismModelSetting;
import com.live2d.sdk.cubism.framework.effect.CubismBreath;
import com.live2d.sdk.cubism.framework.effect.CubismEyeBlink;
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
import com.live2d.sdk.cubism.framework.rendering.CubismRenderer;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenSurfaceAndroid;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRendererAndroid;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LAppModel extends CubismUserModel {
    private static final String TAG = "LAppModel";
    
    public LAppModel() {
        Log.d(TAG, "LAppModel constructor called");
        if (LAppDefine.MOC_CONSISTENCY_VALIDATION_ENABLE) {
            mocConsistency = true;
        }

        if (LAppDefine.MOTION_CONSISTENCY_VALIDATION_ENABLE) {
            motionConsistency = true;
        }

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            debugMode = true;
        }
        
        CubismIdManager idManager = CubismFramework.getIdManager();

        idParamAngleX = idManager.getId(ParameterId.ANGLE_X.getId());
        idParamAngleY = idManager.getId(ParameterId.ANGLE_Y.getId());
        idParamAngleZ = idManager.getId(ParameterId.ANGLE_Z.getId());
        idParamBodyAngleX = idManager.getId(ParameterId.BODY_ANGLE_X.getId());
        idParamEyeBallX = idManager.getId(ParameterId.EYE_BALL_X.getId());
        idParamEyeBallY = idManager.getId(ParameterId.EYE_BALL_Y.getId());

        Log.d(TAG, "LAppModel constructor completed");
    }

    public void loadAssets(final String dir, final String fileName) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("load model setting: " + fileName);
        }

        modelHomeDirectory = dir;
        String filePath = modelHomeDirectory + fileName;

        // 读取json
        byte[] buffer = createBuffer(filePath);

        ICubismModelSetting setting = new CubismModelSettingJson(buffer);

        // Setup model
        setupModel(setting);

        if (model == null) {
            LAppPal.printLog("Failed to loadAssets().");
            return;
        }

        // Setup renderer.
        CubismRenderer renderer = CubismRendererAndroid.create();
        setupRenderer(renderer);

        setupTextures();
    }

    /**
     * 删除LAppModel拥有的模型。
     */
    public void deleteModel() {
        delete();
    }

    /**
     * 模型的更新处理。根据模型的参数决定绘制状态
     */
    public void update() {
        final float deltaTimeSeconds = LAppPal.getDeltaTime();
        userTimeSeconds += deltaTimeSeconds;

        dragManager.update(deltaTimeSeconds);
        dragX = dragManager.getX();
        dragY = dragManager.getY();

        // 是否有通过动作更新参数
        boolean isMotionUpdated = false;

//         加载上次保存的状态
        model.loadParameters();

        // 如果没有播放动作，则从待机动作中随机播放
        if (motionManager.isFinished()) {
            startRandomMotion(LAppDefine.MotionGroup.IDLE.getId(), LAppDefine.Priority.IDLE.getPriority());
        } else {
            // 更新动作
            isMotionUpdated = motionManager.updateMotion(model, deltaTimeSeconds);
        }

        // 保存模型状态
        model.saveParameters();

        // 不透明度
        opacity = model.getModelOpacity();

        // eye blink
        // 仅在没有主要动作更新时才眨眼
        if (!isMotionUpdated) {
            if (eyeBlink != null) {
                eyeBlink.updateParameters(model, deltaTimeSeconds);
            }
        }

        // expression
        if (expressionManager != null) {
            // 通过表情更新参数（相对变化）
            expressionManager.updateMotion(model, deltaTimeSeconds);
        }

        // 拖拽跟随功能
        // 通过拖拽调整面部朝向
        model.addParameterValue(idParamAngleX, dragX * 30); // 添加-30到30的值
        model.addParameterValue(idParamAngleY, dragY * 30);
        model.addParameterValue(idParamAngleZ, dragX * dragY * (-30));

        // 通过拖拽调整身体朝向
        model.addParameterValue(idParamBodyAngleX, dragX * 10); // 添加-10到10的值

        // 通过拖拽调整眼部朝向
        model.addParameterValue(idParamEyeBallX, dragX);  // 添加-1到1的值
        model.addParameterValue(idParamEyeBallY, dragY);

        // Breath Function
        if (breath != null) {
            breath.updateParameters(model, deltaTimeSeconds);
        }

        // Physics Setting
        if (physics != null) {
            physics.evaluate(model, deltaTimeSeconds);
        }

        // Lip Sync Setting
        if (lipSync) {
            // 实时进行唇形同步时，从系统获取音量并在0~1范围内输入值
            float value = 0.0f;

            for (int i = 0; i < lipSyncIds.size(); i++) {
                CubismId lipSyncId = lipSyncIds.get(i);
                model.addParameterValue(lipSyncId, value, 0.8f);
            }
        }

        // Pose Setting
        if (pose != null) {
            pose.updateParameters(model, deltaTimeSeconds);
        }

        model.update();
    }

    /**
     * 开始播放指定动作。
     * 当未传递回调函数时，将其作为空值调用该方法。
     *
     * @param group 动作组名
     * @param number 组内编号
     * @param priority 优先级
     * @return 返回开始的动作识别号。在用于判断个别动作是否结束的isFinished()参数中使用。无法开始时返回"-1"
     */
    public int startMotion(final String group, int number, int priority) {
        return startMotion(group, number, priority, null, null);
    }

    /**
     * 开始播放指定动作。
     *
     * @param group 动作组名
     * @param number 组内编号
     * @param priority 优先级
     * @param onFinishedMotionHandler 动作播放结束时调用的回调函数。为null时不调用。
     * @return 返回开始的动作识别号。在用于判断个别动作是否结束的isFinished()参数中使用。无法开始时返回"-1"
     */
    public int startMotion(final String group,
                           int number,
                           int priority,
                           IFinishedMotionCallback onFinishedMotionHandler,
                           IBeganMotionCallback onBeganMotionHandler
    ) {
        if (priority == LAppDefine.Priority.FORCE.getPriority()) {
            motionManager.setReservationPriority(priority);
        } else if (!motionManager.reserveMotion(priority)) {
            if (debugMode) {
                LAppPal.printLog("Cannot start motion.");
            }
            return -1;
        }

        // ex) idle_0
        String name = group + "_" + number;

        CubismMotion motion = (CubismMotion) motions.get(name);

        if (motion == null) {
            String fileName = modelSetting.getMotionFileName(group, number);
            if (!fileName.equals("")) {
                String path = modelHomeDirectory + fileName;

                byte[] buffer;
                buffer = createBuffer(path);

                motion = loadMotion(buffer, onFinishedMotionHandler, onBeganMotionHandler, motionConsistency);
                if (motion != null) {
                    final float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, number);

                    if (fadeInTime != -1.0f) {
                        motion.setFadeInTime(fadeInTime);
                    }

                    final float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, number);
                    if (fadeOutTime != -1.0f) {
                        motion.setFadeOutTime(fadeOutTime);
                    }

                    motion.setEffectIds(eyeBlinkIds, lipSyncIds);
                } else {
                    CubismDebug.cubismLogError("Can't start motion %s", path);

                    // 重置未能加载的动作的ReservePriority。
                    motionManager.setReservationPriority(LAppDefine.Priority.NONE.getPriority());
                    return -1;
                }
            }
        } else {
            motion.setBeganMotionHandler(onBeganMotionHandler);
            motion.setFinishedMotionHandler(onFinishedMotionHandler);
        }

        // load sound files
        String voice = modelSetting.getMotionSoundFileName(group, number);
        if (!voice.equals("")) {
            String path = modelHomeDirectory + voice;

            // 在另一个线程中播放声音
            LAppWavFileHandler voicePlayer = new LAppWavFileHandler(path);
            voicePlayer.start();
        }

        if (debugMode) {
            LAppPal.printLog("start motion: " + group + "_" + number);
        }
        return motionManager.startMotionPriority(motion, priority);
    }

    /**
     * 开始播放随机选择的动作。
     * 当未传递回调函数时，将其作为空值调用该方法。
     *
     * @param group 动作组名
     * @param priority 优先级
     * @return 开始的动作识别号。在用于判断个别动作是否结束的isFinished()参数中使用。无法开始时返回"-1"
     */
    public int startRandomMotion(final String group, int priority) {
        return startRandomMotion(group, priority, null, null);
    }

    /**
     * 开始播放随机选择的动作。
     *
     * @param group 动作组名
     * @param priority 优先级
     * @param onFinishedMotionHandler 动作播放结束时调用的回调函数。为null时不调用。
     * @return 返回开始的动作识别号。在用于判断个别动作是否结束的isFinished()参数中使用。无法开始时返回-1
     */
    public int startRandomMotion(final String group, int priority, IFinishedMotionCallback onFinishedMotionHandler, IBeganMotionCallback onBeganMotionHandler) {
        if (modelSetting.getMotionCount(group) == 0) {
            return -1;
        }

        Random random = new Random();
        int number = random.nextInt(Integer.MAX_VALUE) % modelSetting.getMotionCount(group);

        return startMotion(group, number, priority, onFinishedMotionHandler, onBeganMotionHandler);
    }

    public void draw(CubismMatrix44 matrix) {
        if (model == null) {
            return;
        }

        // 为避免定义缓存变量，使用multiply()而不是multiplyByMatrix()。
        CubismMatrix44.multiply(
            modelMatrix.getArray(),
            matrix.getArray(),
            matrix.getArray()
        );

        this.<CubismRendererAndroid>getRenderer().setMvpMatrix(matrix);
        this.<CubismRendererAndroid>getRenderer().drawModel();
    }

    /**
     * 碰撞判定测试
     * 从指定ID的顶点列表计算矩形，并判断坐标是否在矩形范围内
     *
     * @param hitAreaName 要测试碰撞判定的对象ID
     * @param x 判定用的x坐标
     * @param y 判定用的y坐标
     * @return 碰撞则返回true
     */
    public boolean hitTest(final String hitAreaName, float x, float y) {
        // 透明时无碰撞判定
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
        // 不存在时返回false
        return false;
    }

    /**
     * 设置指定的表情动作
     *
     * @param expressionID 表情动作的ID
     */
    public void setExpression(final String expressionID) {
        ACubismMotion motion = expressions.get(expressionID);

        if (debugMode) {
            LAppPal.printLog("expression: " + expressionID);
        }

        if (motion != null) {
            expressionManager.startMotionPriority(motion, LAppDefine.Priority.FORCE.getPriority());
        } else {
            if (debugMode) {
                LAppPal.printLog("expression " + expressionID + "is null");
            }
        }
    }

    /**
     * 设置随机选择的表情动作
     */
    public void setRandomExpression() {
        if (expressions.size() == 0) {
            return;
        }

        Random random = new Random();
        int number = random.nextInt(Integer.MAX_VALUE) % expressions.size();

        int i = 0;
        for (String key : expressions.keySet()) {
            if (i == number) {
                setExpression(key);
                return;
            }
            i++;
        }
    }

    public CubismOffscreenSurfaceAndroid getRenderingBuffer() {
        return renderingBuffer;
    }

    /**
     * 检查.moc3文件的一致性。
     *
     * @param mocFileName MOC3文件名
     * @return MOC3是否具有一致性。一致则返回true。
     */
    public boolean hasMocConsistencyFromFile(String mocFileName) {
        assert mocFileName != null && !mocFileName.isEmpty();

        String path = mocFileName;
        path = modelHomeDirectory + path;

        byte[] buffer = createBuffer(path);
        boolean consistency = CubismMoc.hasMocConsistency(buffer);

        if (!consistency) {
            CubismDebug.cubismLogInfo("Inconsistent MOC3.");
        } else {
            CubismDebug.cubismLogInfo("Consistent MOC3.");
        }

        return consistency;
    }

    private static byte[] createBuffer(final String path) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("create buffer: " + path);
        }
        return LAppPal.loadFileAsBytes(path);
    }

    /**
     * 设置渲染器
     * @param renderer 渲染器
     */
    @Override
    public void setupRenderer(CubismRenderer renderer) {
        super.setupRenderer(renderer);
        // 禁用高精度遮罩以避免clippingContext为null的问题
        renderer.isUsingHighPrecisionMask(false);
        
        // 设置渲染器参数
        renderer.setAnisotropy(0);
    }

    // 从model3.json生成模型
    private void setupModel(ICubismModelSetting setting) {
        Log.d(TAG, "setupModel: Starting model setup");
        modelSetting = setting;

        isUpdated = true;
        isInitialized = false;
        Log.d(TAG, "setupModel: Initial flags set - isUpdated=" + isUpdated + ", isInitialized=" + isInitialized);

        // Load Cubism Model
        {
            String fileName = modelSetting.getModelFileName();
            Log.d(TAG, "setupModel: Model file name: " + fileName);
            if (!fileName.equals("")) {
                String path = modelHomeDirectory + fileName;
                Log.d(TAG, "setupModel: Loading model from path: " + path);

                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("create model: " + modelSetting.getModelFileName());
                }

                byte[] buffer = createBuffer(path);
                Log.d(TAG, "setupModel: Buffer created, null=" + (buffer == null));
                if (buffer != null) {
                    Log.d(TAG, "setupModel: Buffer size: " + buffer.length);
                }
                
                loadModel(buffer, mocConsistency);
                Log.d(TAG, "setupModel: Model loaded");
            } else {
                Log.e(TAG, "setupModel: Model file name is empty");
            }
        }

        // load expression files(.exp3.json)
        {
            int expressionCount = modelSetting.getExpressionCount();
            Log.d(TAG, "setupModel: Expression count: " + expressionCount);
            if (expressionCount > 0) {
                final int count = expressionCount;

                for (int i = 0; i < count; i++) {
                    String name = modelSetting.getExpressionName(i);
                    String path = modelSetting.getExpressionFileName(i);
                    Log.d(TAG, "setupModel: Loading expression - name=" + name + ", path=" + path);
                    path = modelHomeDirectory + path;

                    byte[] buffer = createBuffer(path);
                    Log.d(TAG, "setupModel: Expression buffer created, null=" + (buffer == null));
                    CubismExpressionMotion motion = loadExpression(buffer);
                    Log.d(TAG, "setupModel: Expression motion loaded, null=" + (motion == null));

                    if (motion != null) {
                        expressions.put(name, motion);
                        Log.d(TAG, "setupModel: Added expression to map: " + name);
                    }
                }
            }
        }

        // Physics
        {
            String path = modelSetting.getPhysicsFileName();
            Log.d(TAG, "setupModel: Physics file path: " + path);
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                Log.d(TAG, "setupModel: Loading physics from: " + modelPath);
                byte[] buffer = createBuffer(modelPath);
                Log.d(TAG, "setupModel: Physics buffer created, null=" + (buffer == null));

                loadPhysics(buffer);
                Log.d(TAG, "setupModel: Physics loaded");
            }
        }

        // Pose
        {
            String path = modelSetting.getPoseFileName();
            Log.d(TAG, "setupModel: Pose file path: " + path);
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                Log.d(TAG, "setupModel: Loading pose from: " + modelPath);
                byte[] buffer = createBuffer(modelPath);
                Log.d(TAG, "setupModel: Pose buffer created, null=" + (buffer == null));

                loadPose(buffer);
                Log.d(TAG, "setupModel: Pose loaded");
            }
        }

        // Load eye blink data
        if (modelSetting.getEyeBlinkParameterCount() > 0) {
            eyeBlink = CubismEyeBlink.create(modelSetting);
        }

        // Load Breath Data
        breath = CubismBreath.create();
        List<CubismBreath.BreathParameterData> breathParameters = new ArrayList<CubismBreath.BreathParameterData>();

        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleX, 0.0f, 15.0f, 6.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleY, 0.0f, 8.0f, 3.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleZ, 0.0f, 10.0f, 5.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamBodyAngleX, 0.0f, 4.0f, 15.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(CubismFramework.getIdManager().getId(ParameterId.BREATH.getId()), 0.5f, 0.5f, 3.2345f, 0.5f));

        breath.setParameters(breathParameters);

        // Load UserData
        {
            String path = modelSetting.getUserDataFile();
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                byte[] buffer = createBuffer(modelPath);
                loadUserData(buffer);
            }
        }


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

        // Set layout
        Map<String, Float> layout = new HashMap<String, Float>();

        // 如果存在布局信息，则根据该信息设置模型矩阵
        if (modelSetting.getLayoutMap(layout)) {
            modelMatrix.setupFromLayout(layout);
        }

        model.saveParameters();

        // Load motions
        for (int i = 0; i < modelSetting.getMotionGroupCount(); i++) {
            String group = modelSetting.getMotionGroupName(i);
            preLoadMotionGroup(group);
        }

        motionManager.stopAllMotions();

        isUpdated = false;
        isInitialized = true;
    }

    /**
     * 从组名批量加载动作数据。
     * 动作数据的名称从ModelSetting获取。
     *
     * @param group 动作数据的组名
     **/
    private void preLoadMotionGroup(final String group) {
        final int count = modelSetting.getMotionCount(group);

        for (int i = 0; i < count; i++) {
            // ex) idle_0
            String name = group + "_" + i;

            String path = modelSetting.getMotionFileName(group, i);
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;

                if (debugMode) {
                    LAppPal.printLog("load motion: " + path + "==>[" + group + "_" + i + "]");
                }

                byte[] buffer;
                buffer = createBuffer(modelPath);

                // 如果无法加载动作，则跳过该过程。
                CubismMotion tmp = loadMotion(buffer, motionConsistency);
                if (tmp == null) {
                    continue;
                }

                final float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, i);

                if (fadeInTime != -1.0f) {
                    tmp.setFadeInTime(fadeInTime);
                }

                final float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, i);

                if (fadeOutTime != -1.0f) {
                    tmp.setFadeOutTime(fadeOutTime);
                }

                tmp.setEffectIds(eyeBlinkIds, lipSyncIds);
                motions.put(name, tmp);
            }
        }
    }

    /**
     * 将纹理加载到OpenGL的纹理单元
     */
    private void setupTextures() {
        for (int modelTextureNumber = 0; modelTextureNumber < modelSetting.getTextureCount(); modelTextureNumber++) {
            // 如果纹理名称为空字符串，则跳过加载和绑定处理
            if (modelSetting.getTextureFileName(modelTextureNumber).equals("")) {
                continue;
            }

            // 将纹理加载到OpenGL ES的纹理单元
            String texturePath = modelSetting.getTextureFileName(modelTextureNumber);
            texturePath = modelHomeDirectory + texturePath;

            LAppTextureManager.TextureInfo texture =
                LAppDelegate.getInstance()
                            .getTextureManager()
                            .createTextureFromPngFile(texturePath);
            final int glTextureNumber = texture.id;

            this.<CubismRendererAndroid>getRenderer().bindTexture(modelTextureNumber, glTextureNumber);

            if (LAppDefine.PREMULTIPLIED_ALPHA_ENABLE) {
                this.<CubismRendererAndroid>getRenderer().isPremultipliedAlpha(true);
            } else {
                this.<CubismRendererAndroid>getRenderer().isPremultipliedAlpha(false);
            }
        }
    }

    private ICubismModelSetting modelSetting;
    /**
     * 模型的主目录
     */
    private String modelHomeDirectory;
    /**
     * delta时间的累计值[秒]
     */
    private float userTimeSeconds;

    /**
     * 模型设置的眨眼功能用参数ID
     */
    private final List<CubismId> eyeBlinkIds = new ArrayList<CubismId>();
    /**
     * 模型设置的唇形同步功能用参数ID
     */
    private final List<CubismId> lipSyncIds = new ArrayList<CubismId>();
    /**
     * 已加载动作的映射
     */
    private final Map<String, ACubismMotion> motions = new HashMap<String, ACubismMotion>();
    /**
     * 已加载表情的映射
     */
    private final Map<String, ACubismMotion> expressions = new HashMap<String, ACubismMotion>();

    /**
     * 参数ID: ParamAngleX
     */
    private final CubismId idParamAngleX;
    /**
     * 参数ID: ParamAngleY
     */
    private final CubismId idParamAngleY;
    /**
     * 参数ID: ParamAngleZ
     */
    private final CubismId idParamAngleZ;
    /**
     * 参数ID: ParamBodyAngleX
     */
    private final CubismId idParamBodyAngleX;
    /**
     * 参数ID: ParamEyeBallX
     */
    private final CubismId idParamEyeBallX;
    /**
     * 参数ID: ParamEyeBallY
     */
    private final CubismId idParamEyeBallY;

    /**
     * 帧缓冲区以外的绘制目标
     */
    private final CubismOffscreenSurfaceAndroid renderingBuffer = new CubismOffscreenSurfaceAndroid();
}