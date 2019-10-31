package com.gmyboy.plugin.deployer

import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * 自动化部署app具体上插实现类
 * Created by gmy on 19-10-23 17:37.
 * E-mail me via gmyboy@qq.com
 */
class GUploader implements Plugin<Project> {
    private Project mProject

    public static final String UPLOAD_URL = "http://a.ssei.cn/sseiserver/admin/deployer/"
    private String mUrl

    @Override
    void apply(Project project) {
        this.mProject = project
        mProject.extensions.create("deployer", ScriptExtension)

        // 取得外部参数
        if (mProject.android.hasProperty("applicationVariants")) { // For android application.
            mProject.android.applicationVariants.all { variant ->
                String variantName = variant.name.capitalize()

                // Check for execution
                if (false == mProject.deployer.enable) {
                    mProject.logger.error("deployer:gradle enable is false, if you want to auto upload apk file, you should set the enable = true")
                    return
                }

                // Create task.
                Task betaTask = createUploadTask(variant)

                // Check autoUpload
                if (!mProject.deployer.autoUpload) {
                    // dependsOn task
                    betaTask.dependsOn mProject.tasks["assemble${variantName}"]
                } else {
                    // autoUpload after assemble
                    mProject.tasks["assemble${variantName}"].doLast {
                        // if debug model and debugOn = false no execute upload
                        if (variantName.contains("Debug") && !mProject.deployer.debugOn) {
                            println("deployer:the option debugOn is closed, if you want to upload apk file on debug model, you can set debugOn = true to open it")
                            return
                        }

                        if (variantName.contains("Release")) {
                            println("deployer:the option autoUpload is opened, it will auto upload the release to the a.ssei.cn")
                        }
                        uploadApk(generateUploadInfo(variant), variantName)
                    }
                }
            }
        }
    }

    /**
     * generate upload info
     * @param variant
     * @return
     */
    UploadInfo generateUploadInfo(Object variant) {
//        def manifestFile = variant.outputs.processManifest.manifestOutputFile[0]
//        println("-> Manifest: " + manifestFile)
//        println("VersionCode: " + variant.getVersionCode() + " VersionName: " + variant.getVersionName())

        UploadInfo uploadInfo = new UploadInfo()
        uploadInfo.appId = mProject.deployer.appId
        uploadInfo.appKey = mProject.deployer.appKey

        if (mProject.deployer.desc == null) {
            uploadInfo.description = ""
        } else {
            uploadInfo.description = mProject.deployer.desc
        }

        uploadInfo.userName = mProject.deployer.user
        uploadInfo.userPwd = mProject.deployer.password

        // if you not set apkFile, default get the assemble output file
        if (mProject.deployer.apkFile != null) {
            uploadInfo.file = mProject.deployer.apkFile
            uploadInfo.outputFile = new File(mProject.deployer.apkFile).getParent() + File.separator + "output.json"
            println("deployer:custom apk absolutepath :" + mProject.deployer.apkFile)
        } else {
            File apkFile = variant.outputs[0].outputFile
            uploadInfo.file = apkFile.getAbsolutePath()
            uploadInfo.outputFile = apkFile.getParent() + File.separator + "output.json"
            println("deployer:default build apk file absolutepath :" + apkFile.getAbsolutePath())
        }

        if (mProject.deployer.url != null) {
            mUrl = mProject.deployer.url
        } else {
            mUrl = UPLOAD_URL
        }

        return uploadInfo
    }

    /**
     * 创建上传任务
     *
     * @param variant 编译参数
     * @return
     */
    private Task createUploadTask(Object variant) {
        String variantName = variant.name.capitalize()
        Task uploadTask = mProject.tasks.create("deploy${variantName}ApkFile").doLast {
            // if debug model and debugOn = false no execute upload
            if (variantName.contains("Debug") && !mProject.deployer.debugOn) {
                println("deployer:the option debugOn is closed, if you want to upload apk file on debug model, you can set debugOn = true to open it")
                return
            }
            uploadApk(generateUploadInfo(variant))
        }
        println("deployer:create deploy${variantName}ApkFile task")
        return uploadTask
    }

    /**
     *  上传apk
     * @param uploadInfo
     * @return
     */
    boolean uploadApk(UploadInfo uploadInfo, String variantName) {
        if (variantName.contains("Release")) {
            mUrl += "release"
        } else {
            mUrl += "debug"
        }

        if (uploadInfo.appId == null) {
            mProject.logger.error("deployer:please set the app id, eg: appId = \"900037672\"")
            return false
        }

        if (uploadInfo.appKey == null) {
            mProject.logger.error("deployer:please set app key, eg: appKey = \"bQvYLRrBNiqUctfi\"")
            return false
        }

        File mFile = new File(uploadInfo.file)
        File mFileOutput = new File(uploadInfo.outputFile)
        if (!mFile.exists() || !mFileOutput.exists()) {
            mProject.logger.error("deployer:apk file or output.json not found")
            return false
        }

        println("deployer:apk start uploading")
        println("deployer:" + uploadInfo.toString())

        if (!post(mUrl, uploadInfo.file, uploadInfo.outputFile, uploadInfo)) {
            project.logger.error("deployer:failed to upload")
            return false
        } else {
            println("deployer:upload apk success")
            return true
        }
    }
    /**
     * 上传apk
     * @param url 地址
     * @param filePath 文件路径
     * @param uploadInfo 更新信息
     * @return
     */
    boolean post(String url, String filePath, String outputFilePath, UploadInfo uploadInfo) {
        HttpURLConnectionUtil connectionUtil = new HttpURLConnectionUtil(url, GParams.HTTPMETHOD_POST)

        connectionUtil.addTextParameter(GParams.APP_ID, uploadInfo.appId)
        connectionUtil.addTextParameter(GParams.APP_KEY, uploadInfo.appKey)

        connectionUtil.addTextParameter(GParams.USERNAME, uploadInfo.userName)
        connectionUtil.addTextParameter(GParams.USERPWD, uploadInfo.userPwd)

        connectionUtil.addTextParameter(GParams.DESCRIPTION, uploadInfo.description)
        connectionUtil.addFileParameter(GParams.FILE, new File(filePath))
        connectionUtil.addFileParameter(GParams.OUTPUTFILE, new File(outputFilePath))

        String result = new String(connectionUtil.post(), "UTF-8")
        def data = new JsonSlurper().parseText(result.substring(1))
        if ("10000".equals(data.error)) {
//            println("deployer:upload result" + data.msg)
            return true
        }
        return false
    }

    /**
     * 传给网络请求的封装类
     */
    static class UploadInfo {
        private String appId
        private String appKey

        private String userName
        private String userPwd

        private String file
        private String description
        private String outputFile

        @Override
        String toString() {
            return "uploadInfo{" +
                    "appId='" + appId + '\'' +
                    ", appKey='" + appKey + '\'' +
                    ", apkFile='" + file + '\'' +
                    ", outputFile='" + outputFile + '\'' +
                    ", description='" + description + '\'' +
                    ", users='" + userName + '\'' +
                    ", password='" + userPwd + '\'' +
                    '}'
        }
    }
}
