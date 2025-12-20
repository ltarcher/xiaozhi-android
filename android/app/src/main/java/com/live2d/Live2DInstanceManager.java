/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.app.Activity;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live2D多实例管理器
 * 负责管理多个Live2D实例的创建、销毁和状态维护
 */
public class Live2DInstanceManager {
    private static final String TAG = "Live2DInstanceManager";
    
    /**
     * 单例实例
     */
    private static Live2DInstanceManager s_instance;
    
    /**
     * 实例ID到内部索引的映射
     */
    private final Map<String, Integer> instanceToIndex;
    
    /**
     * 内部索引到实例数据的映射
     */
    private final Map<Integer, Live2DInstanceData> indexToInstance;
    
    /**
     * 下一个可用的索引
     */
    private final AtomicInteger nextIndex;
    
    /**
     * 实例数据结构
     */
    public static class Live2DInstanceData {
        public String instanceId;
        public LAppDelegate appDelegate;
        public LAppView view;
        public LAppLive2DManager manager;
        public boolean isActive;
        public Activity activity;
        
        public Live2DInstanceData(String instanceId, Activity activity) {
            this.instanceId = instanceId;
            this.activity = activity;
            this.isActive = false;
        }
        
        /**
         * 初始化实例组件
         */
        public void initialize() {
            Log.d(TAG, "Initializing instance: " + instanceId);
            
            // 创建应用委托
            appDelegate = new LAppDelegate();
            appDelegate.setInstanceId(instanceId);
            
            // 初始化委托
            appDelegate.onStart(activity);
            
            // 获取视图和管理器引用
            view = appDelegate.getView();
            manager = LAppLive2DManager.getInstance(instanceId);
            
            Log.d(TAG, "Instance initialization completed: " + instanceId);
        }
        
        /**
         * 销毁实例组件
         */
        public void dispose() {
            Log.d(TAG, "Disposing instance: " + instanceId);
            
            if (appDelegate != null) {
                try {
                    appDelegate.onStop();
                    appDelegate.onDestroy();
                } catch (Exception e) {
                    Log.e(TAG, "Error disposing delegate for instance: " + instanceId, e);
                }
            }
            
            // 清理引用
            view = null;
            manager = null;
            appDelegate = null;
            activity = null;
            
            Log.d(TAG, "Instance disposed: " + instanceId);
        }
        
        /**
         * 激活实例
         */
        public void activate() {
            Log.d(TAG, "Activating instance: " + instanceId);
            this.isActive = true;
            
            if (appDelegate != null) {
                // 重新初始化以确保状态正确
                try {
                    appDelegate.onStart(activity);
                } catch (Exception e) {
                    Log.e(TAG, "Error activating instance: " + instanceId, e);
                }
            }
        }
        
        /**
         * 停用实例
         */
        public void deactivate() {
            Log.d(TAG, "Deactivating instance: " + instanceId);
            this.isActive = false;
            
            if (appDelegate != null) {
                try {
                    appDelegate.onPause();
                } catch (Exception e) {
                    Log.e(TAG, "Error deactivating instance: " + instanceId, e);
                }
            }
        }
    }
    
    /**
     * 获取单例实例
     */
    public static Live2DInstanceManager getInstance() {
        if (s_instance == null) {
            Log.d(TAG, "Creating new Live2DInstanceManager instance");
            s_instance = new Live2DInstanceManager();
        }
        return s_instance;
    }
    
    /**
     * 释放单例实例
     */
    public static void releaseInstance() {
        Log.d(TAG, "Releasing Live2DInstanceManager instance");
        if (s_instance != null) {
            s_instance.disposeAllInstances();
            s_instance = null;
        }
    }
    
    /**
     * 构造函数
     */
    private Live2DInstanceManager() {
        instanceToIndex = new HashMap<>();
        indexToInstance = new HashMap<>();
        nextIndex = new AtomicInteger(0);
        Log.d(TAG, "Live2DInstanceManager created");
    }
    
    /**
     * 创建新的Live2D实例
     * 
     * @param instanceId 实例ID
     * @param activity Activity上下文
     * @return 是否创建成功
     */
    public boolean createInstance(String instanceId, Activity activity) {
        Log.d(TAG, "Creating instance: " + instanceId);
        
        if (instanceId == null || activity == null) {
            Log.e(TAG, "Cannot create instance: instanceId or activity is null");
            return false;
        }
        
        if (instanceToIndex.containsKey(instanceId)) {
            Log.w(TAG, "Instance already exists: " + instanceId);
            return true; // 已存在认为成功
        }
        
        try {
            int index = nextIndex.getAndIncrement();
            Live2DInstanceData instanceData = new Live2DInstanceData(instanceId, activity);
            instanceData.initialize();
            
            instanceToIndex.put(instanceId, index);
            indexToInstance.put(index, instanceData);
            
            Log.d(TAG, "Instance created successfully: " + instanceId + " -> index: " + index);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating instance: " + instanceId, e);
            return false;
        }
    }
    
    /**
     * 获取指定实例的数据
     * 
     * @param instanceId 实例ID
     * @return 实例数据，如果不存在则返回null
     */
    public Live2DInstanceData getInstance(String instanceId) {
        if (instanceId == null) {
            return null;
        }
        
        Integer index = instanceToIndex.get(instanceId);
        if (index == null) {
            return null;
        }
        
        return indexToInstance.get(index);
    }
    
    /**
     * 销毁指定的实例
     * 
     * @param instanceId 实例ID
     * @return 是否销毁成功
     */
    public boolean destroyInstance(String instanceId) {
        Log.d(TAG, "Destroying instance: " + instanceId);
        
        if (instanceId == null) {
            Log.e(TAG, "Cannot destroy instance: instanceId is null");
            return false;
        }
        
        Integer index = instanceToIndex.get(instanceId);
        if (index == null) {
            Log.w(TAG, "Instance does not exist: " + instanceId);
            return false;
        }
        
        try {
            Live2DInstanceData instanceData = indexToInstance.get(index);
            if (instanceData != null) {
                instanceData.dispose();
            }
            
            instanceToIndex.remove(instanceId);
            indexToInstance.remove(index);
            
            Log.d(TAG, "Instance destroyed successfully: " + instanceId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error destroying instance: " + instanceId, e);
            return false;
        }
    }
    
    /**
     * 激活指定实例
     * 
     * @param instanceId 实例ID
     * @return 是否激活成功
     */
    public boolean activateInstance(String instanceId) {
        Log.d(TAG, "Activating instance: " + instanceId);
        
        Live2DInstanceData instanceData = getInstance(instanceId);
        if (instanceData == null) {
            Log.e(TAG, "Cannot activate: instance does not exist: " + instanceId);
            return false;
        }
        
        try {
            instanceData.activate();
            Log.d(TAG, "Instance activated successfully: " + instanceId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error activating instance: " + instanceId, e);
            return false;
        }
    }
    
    /**
     * 停用指定实例
     * 
     * @param instanceId 实例ID
     * @return 是否停用成功
     */
    public boolean deactivateInstance(String instanceId) {
        Log.d(TAG, "Deactivating instance: " + instanceId);
        
        Live2DInstanceData instanceData = getInstance(instanceId);
        if (instanceData == null) {
            Log.e(TAG, "Cannot deactivate: instance does not exist: " + instanceId);
            return false;
        }
        
        try {
            instanceData.deactivate();
            Log.d(TAG, "Instance deactivated successfully: " + instanceId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deactivating instance: " + instanceId, e);
            return false;
        }
    }
    
    /**
     * 检查实例是否存在
     * 
     * @param instanceId 实例ID
     * @return 是否存在
     */
    public boolean hasInstance(String instanceId) {
        return instanceId != null && instanceToIndex.containsKey(instanceId);
    }
    
    /**
     * 获取所有实例ID
     * 
     * @return 实例ID集合
     */
    public java.util.Set<String> getAllInstanceIds() {
        return new java.util.HashSet<>(instanceToIndex.keySet());
    }
    
    /**
     * 获取实例数量
     * 
     * @return 实例数量
     */
    public int getInstanceCount() {
        return instanceToIndex.size();
    }
    
    /**
     * 销毁所有实例
     */
    public void disposeAllInstances() {
        Log.d(TAG, "Disposing all instances");
        
        try {
            // 创建副本以避免并发修改
            java.util.Set<String> instanceIds = new java.util.HashSet<>(instanceToIndex.keySet());
            
            for (String instanceId : instanceIds) {
                destroyInstance(instanceId);
            }
            
            Log.d(TAG, "All instances disposed");
        } catch (Exception e) {
            Log.e(TAG, "Error disposing all instances", e);
        }
    }
    
    /**
     * 获取默认实例ID（用于向后兼容）
     * 
     * @return 默认实例ID
     */
    public String getDefaultInstanceId() {
        // 返回第一个创建的实例ID
        for (String instanceId : instanceToIndex.keySet()) {
            return instanceId;
        }
        return null;
    }
    
    /**
     * 为向后兼容提供默认实例
     * 
     * @return 默认的LAppDelegate
     */
    public LAppDelegate getDefaultAppDelegate() {
        Live2DInstanceData instanceData = getInstance(getDefaultInstanceId());
        return instanceData != null ? instanceData.appDelegate : null;
    }
}