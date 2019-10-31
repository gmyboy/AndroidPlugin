package com.gmyboy.plugin.deployer;

/**
 * 用于获取gradle script 传过来的参数
 * Created by gmy on 19-10-24 09:23.
 * E-mail me via gmyboy@qq.com
 */
class ScriptExtension {
    public String url = null

    public String appId = null // AppID 【必选】
    public String appKey = null // AppKey 【必选】

    // 【接口参数】
    public String desc = null // 版本描述
    public String user = null //
    public String password = null // 密码(如果公开范围是"密码"需设置)

    // 【插件配置】
    public String apkFile = null // 指定上传的apk文件
    public Boolean enable = true // 插件开关
    public Boolean autoUpload = false // 是否自动上传
    public Boolean debugOn = false // debug模式是否上传
}
