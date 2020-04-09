package com.ztgeo.suqian.utils;//package com.ztgeo.suqian.sqdata.common.util;

import org.apache.commons.net.ftp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * FTP工具
 * 1、上传、下载
 * 2、获取目录（共享平台、不动产）
 * 3、获取文件名（共享平台）
 * 4、获取不动产FTP地址
 */
@Repository
public class FTPUtil {
    // LOG
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Value(value = "${customAttributes.ftpIp}")
    private String BDCURL;
    @Value(value = "${customAttributes.ftpPort}")
    private String BDCPORT;
    @Value(value = "${customAttributes.ftpUsername}")
    private String BDCUSERNAME;
    @Value(value = "${customAttributes.ftpPassword}")
    private String BDCPASSWORD;

//    @Value(value = "${self.ftp.pt.url}")
//    private String PTURL;
//    @Value(value = "${self.ftp.pt.port}")
//    private String PTPORT;
//    @Value(value = "${self.ftp.pt.username}")
//    private String PTUSERNAME;
//    @Value(value = "${self.ftp.pt.password}")
//    private String PTPASSWORD;

    /**
     * Description: 从FTP服务器下载文件（不动产）
     *
     * @param remotePath FTP服务器上的相对路径
     * @param fileName   要下载的文件名
     * @return
     */
    public byte[] downFileBDCAsByte(String remotePath, String fileName) {
        byte[] pdfBytes = null;
        boolean success = false;
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(BDCURL, Integer.valueOf(BDCPORT));//连接FTP服务器
            //如果采用默认端口，可以使用ftp.connect(url)的方式直接连接FTP服务器
            ftp.login(BDCUSERNAME, BDCPASSWORD);//登录
            reply = ftp.getReplyCode();
            log.info("FTP连接返回：" + reply);
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new RuntimeException("不动产FTP连接失败！");
            }
            //ftp.enterLocalActiveMode(); // 主动
            //ftp.enterLocalPassiveMode(); // 被动

            ftp.changeWorkingDirectory(remotePath);//转移到FTP服务器目录
            FTPFile[] fs = ftp.listFiles();
            for (FTPFile ff : fs) {
                if (ff.getName().equals(fileName)) {
                    InputStream inputStream = ftp.retrieveFileStream(ff.getName());
                    pdfBytes = is2byte(inputStream);
                }
            }
            ftp.logout();
        } catch (IOException e) {
            log.info("发生异常 msg={从不动产FTP下载文件异常}", "FTPUtil-downFileBDC", e);
            throw new RuntimeException("从不动产FTP下载文件异常");
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        if (pdfBytes != null && pdfBytes.length > 0) {
            return pdfBytes;
        } else {
            throw new RuntimeException("未从不动产FTP中获取到对应的电子证书");
        }
    }

//
//    /**
//     * 从共享平台下载附件信息，并保存成字节数组
//     *
//     * @param remotePath 共享平台附件目录
//     * @param fileName   共享平台附件名称
//     * @return
//     */
//    public byte[] downFileGXPTSaveAsBytes(String remotePath, String fileName) {
//        byte[] bytes = new byte[0];
//        try {
//            FTPClient ftp = new FTPClient();
//            int reply;
//            ftp.connect(PTURL, Integer.valueOf(PTPORT));//连接FTP服务器
//            ftp.login(PTUSERNAME, PTPASSWORD);//登录
//            reply = ftp.getReplyCode();
//            log.info("FTP状态码：" + reply);
//            if (!FTPReply.isPositiveCompletion(reply)) {
//                ftp.disconnect();
//            }
//            // 以下为UNIX中FTP设置
////            ftp.setFileType(2);
////            FTPClientConfig conf = new FTPClientConfig("UNIX");
////            ftp.configure(conf);
////            ftp.setRemoteVerificationEnabled(false);
//            // 以上为UNIX中FTP设置
//            ftp.setBufferSize(102400);
//            //ftp.enterLocalActiveMode();
//            ftp.changeWorkingDirectory(remotePath);//转移到FTP服务器目录
//            log.info("切换目录成功");
//            FTPFile[] fs = ftp.listFiles();
//            for (FTPFile ff : fs) {
//                if (ff.getName().equals(fileName)) {
//                    InputStream is = ftp.retrieveFileStream(ff.getName());
//                    bytes = is2byte(is);
//                }
//            }
//            ftp.logout();
//        } catch (Exception e) {
//            log.info("从共享平台获取附件字节数据异常！原因：" + e.getMessage());
//        }
//        return bytes;
//    }

    /**
     * 将附件上传至不动产FTP
     *
     * @param mulu         保存目录（受理编号拆分）
     * @param fileName_ftp FTP文件名（Bin-xxxxxx-xxxxxxxx）
     * @param is           FTP字符流，从byte[]数组转换
     * @return 是否上传成功
     */
    public boolean uploadFileBDC(String mulu, String fileName_ftp, InputStream is) {
        boolean success;
        System.out.println("ip"+BDCURL);
        System.out.println("port"+BDCPORT);
        System.out.println("usename"+BDCUSERNAME);
        System.out.println("pw"+BDCPASSWORD);
        try {
            // 上传文件
            FTPClient ftp = new FTPClient();
            ftp.connect(BDCURL, Integer.valueOf(BDCPORT));
            ftp.login(BDCUSERNAME, BDCPASSWORD);
            int reply = ftp.getReplyCode();
            log.info("FTP连接返回：" + reply);
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
            }
//            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftp.setBufferSize(10240 * 10240);
            boolean flag = ftp.changeWorkingDirectory(mulu);
            if (!flag) {
                mkDirs(ftp, mulu);
            }
            ftp.changeWorkingDirectory(mulu);
            ftp.storeFile(fileName_ftp, is);
            ftp.logout();
            is.close();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("FTP上传文件异常！" + e.getMessage());
        }
        return success;
    }

//    /**
//     * @param path     FTP服务器保存目录
//     * @param filename 上传到FTP服务器上的文件名
//     * @param input    输入流
//     * @return 成功返回true，否则返回false
//     * @deprecated 上传文件到FTP（共享平台）
//     */
//    public boolean uploadFileGXPT(String path, String filename, InputStream input) {
//        log.info("上传文件从共享平台FTP");
//        boolean success = false;
//        FTPClient ftp = new FTPClient();
//        try {
//            int reply;
//            ftp.connect(PTURL, Integer.valueOf(PTPORT));//连接FTP服务器
//            //如果采用默认端口，可以使用ftp.connect(url)的方式直接连接FTP服务器
//            ftp.login(PTUSERNAME, PTPASSWORD);//登录
//
//            // 针对UNIX系统设置
//            FTPClientConfig conf = new FTPClientConfig("UNIX");
//            ftp.configure(conf);
//            ftp.setFileType(2);
//
//            ftp.setRemoteVerificationEnabled(false);
//            ftp.setBufferSize(10485760);
//
//            ftp.enterLocalActiveMode();
//
//            reply = ftp.getReplyCode();
//            log.info("FTP连接返回：" + reply);
//            if (!FTPReply.isPositiveCompletion(reply)) {
//                ftp.disconnect();
//                return success;
//            }
//            ftp.changeWorkingDirectory(path);
//            ftp.storeFile(filename, input);
//
//            input.close();
//            ftp.logout();
//            success = true;
//        } catch (IOException e) {
//            log.info("发生异常 msg={上传文件到共享平台失败！}", "FTPUtil-uploadFileGXPT！", e);
//            throw new RuntimeException("上传文件到共享平台失败" + e.getMessage());
//        } finally {
//            if (ftp.isConnected()) {
//                try {
//                    ftp.disconnect();
//                } catch (IOException ioe) {
//                }
//            }
//        }
//        log.info("FTP文件上传共享平台是否成功！" + success);
//        return success;
//    }

    /**
     * @param filePath FTP文件路径
     * @return fale-不存在 true-存在
     */
    public boolean isFTPFileExistBDC(String filePath) {
        FTPClient ftp = new FTPClient();
        try {
            // 连接ftp服务器
            ftp.connect(BDCURL, Integer.valueOf(BDCPORT));
            // 登陆
            ftp.login(BDCUSERNAME, BDCPASSWORD);
            // 检验登陆操作的返回码是否正确
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.disconnect();
                return false;
            }

            ftp.enterLocalActiveMode();
            // 设置文件类型为二进制，与ASCII有区别
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            // 设置编码格式
            ftp.setControlEncoding("GBK");

            // 提取绝对地址的目录以及文件名
            filePath = filePath.replace("ftp://" + BDCURL + ":" + Integer.valueOf(BDCPORT) + "/", "");
            String dir = filePath.substring(0, filePath.lastIndexOf("/"));
            String file = filePath.substring(filePath.lastIndexOf("/") + 1);

            // 进入文件所在目录，注意编码格式，以能够正确识别中文目录
            ftp.changeWorkingDirectory(new String(dir.getBytes("GBK"), FTP.DEFAULT_CONTROL_ENCODING));

            // 检验文件是否存在
            InputStream is = ftp.retrieveFileStream(new String(file.getBytes("GBK"), FTP.DEFAULT_CONTROL_ENCODING));
            if (is == null || ftp.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
                return false;
            }

            if (is != null) {
                is.close();
                ftp.completePendingCommand();
            }
            return true;
        } catch (Exception e) {
            log.info("发生异常 msg={FTP查询附件状态异常！}", "FTPUtil-isFTPFileExist！", e);
            throw new RuntimeException("FTP查询附件状态异常！" + e.getMessage());
        } finally {
            if (ftp != null) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    /**
//     * 对FTP地址进行拆分，民丰FTP地址为全地址----针对共享平台
//     *
//     * @param fileaddress 共享平台FTP全地址：ftp://127.0.0.1/2019/03/06/0b1825a2-45b7-4491-a729-b4e199231300.png
//     * @return 共享平台FTP目录地址：/2019/03/06/
//     */
//    public String getRemotePathInPT_ALL(String fileaddress) {
//        StringBuffer sb = new StringBuffer("/");
//        String afterPath = fileaddress.split(PTURL)[1];
//        String[] afterPaths = afterPath.split("/");
//        for (int i = 1; i < afterPaths.length - 1; i++) {
//            sb.append(afterPaths[i]).append("/");
//        }
//        return sb.toString();
//    }

//    /**
//     * 获取共享平台FTP文件名称------针对共享平台
//     *
//     * @param fileaddress FTP全地址：ftp://127.0.0.1/2019/03/06/0b1825a2-45b7-4491-a729-b4e199231300.png
//     * @return FTP文件名：0b1825a2-45b7-4491-a729-b4e199231300.png
//     */
//    public String getFileNameInPTFTP_ALL(String fileaddress) {
//        String[] afterPaths = fileaddress.split(PTURL)[1].split("/");
//        return afterPaths[afterPaths.length - 1];
//    }


    /**
     * 对FTP地址进行拆分，非全地址
     *
     * @param fileaddress 共享平台FTP全地址：/2019/03/06/0b1825a2-45b7-4491-a729-b4e199231300.png
     * @return 共享平台FTP目录地址：/2019/03/06/
     */
    public String getRemotePathInPT(String fileaddress) {
        StringBuffer sb = new StringBuffer("/");
        String[] afterPaths = fileaddress.split("/");
        for (int i = 1; i < afterPaths.length - 1; i++) {
            sb.append(afterPaths[i]).append("/");
        }
        return sb.toString();
    }

    /**
     * 获取共享平台FTP文件名称------非全地址
     *
     * @param fileaddress FTP全地址：/2019/03/06/0b1825a2-45b7-4491-a729-b4e199231300.png
     * @return FTP文件名：0b1825a2-45b7-4491-a729-b4e199231300.png
     */
    public String getFileNameInPTFTP(String fileaddress) {
        String[] afterPaths = fileaddress.split("/");
        return afterPaths[afterPaths.length - 1];
    }

    //

    /**
     * 获取不动产平台FTP附件存放目录------针对不动产
     *
     * @param slbh 不动产受理编号
     * @return 不动产FTP目录：/2019/03/19/
     */
    public String getRemotePathInBDC(String slbh) {
        return new StringBuffer("/").append(slbh, 0, 4).append("/").append(slbh, 4, 6).append("/").append(slbh, 6, 8).toString();
    }

    /**
     * 获取FTP保存地址，从受理编号进行拆分后获取，文档库DOC_BINFILE中字段
     *
     * @param slbh                  受理编号
     * @param binId                 附件BINID
     * @param attachmentName_suffix 文件后缀
     * @return 返回FTP地址（非全地址）/2019/03/19/Bin-xxxxxxx-xxxxxxxx.jpg
     */
    public String getFTPPathInDB(String slbh, String binId, String attachmentName_suffix) {
        String ftpPath;
        StringBuffer sb = new StringBuffer("/");
        sb.append(slbh, 0, 4).append("/").append(slbh, 4, 6).append("/").append(slbh, 6, 8);
        sb.append("/").append(binId).append(".").append(attachmentName_suffix);
        ftpPath = sb.toString();
        return ftpPath;
    }

    public byte[] getCurrentFileAsByte(byte[] ftpFileBytes, String fileNameInZip) {
        log.info("当前搜索文件名：" + fileNameInZip);
        byte[] currentBytes = null;
        try {
            ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(ftpFileBytes), Charset.forName("GBK"));
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                //log.info("遍历文件名：" + entry.getName());
                if (entry.getName().endsWith(fileNameInZip)) {
                    log.info("匹配到文件");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len = 1024;
                    byte tmp[] = new byte[len];
                    int i;
                    while ((i = zin.read(tmp, 0, len)) > 0) {
                        baos.write(tmp, 0, i);
                    }
                    currentBytes = baos.toByteArray();
                }
                zin.closeEntry();
            }
            zin.close();
        } catch (IOException e) {
            log.info("从ZIP文件流中获取当前附件字符数组异常！", e);
            throw new RuntimeException("从ZIP文件流中获取当前附件字符数组异常！");
        }

        if (currentBytes == null || currentBytes.length <= 0) {
            throw new RuntimeException("从ZIP文件流中获取当前附件字符数组长度为0");
        } else {
            return currentBytes;
        }
    }
//
//    /**
//     * @param filePath FTP文件路径
//     * @return fale-不存在 true-存在
//     */
//    public boolean isFTPFileExist(String filePath) {
//        FTPClient ftp = new FTPClient();
//        try {
//            // 连接ftp服务器
//            ftp.connect(PTURL, Integer.valueOf(PTPORT));
//            // 登陆
//            ftp.login(PTUSERNAME, PTPASSWORD);
//            // 检验登陆操作的返回码是否正确
//            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
//                ftp.disconnect();
//                return false;
//            }
//
//            ftp.enterLocalActiveMode();
//            // 设置文件类型为二进制，与ASCII有区别
//            ftp.setFileType(FTP.BINARY_FILE_TYPE);
//            // 设置编码格式
//            ftp.setControlEncoding("GBK");
//
//            // 提取绝对地址的目录以及文件名
//            filePath = filePath.replace("ftp://" + PTURL + ":" + Integer.valueOf(PTPORT) + "/", "");
//            String dir = filePath.substring(0, filePath.lastIndexOf("/"));
//            String file = filePath.substring(filePath.lastIndexOf("/") + 1);
//
//            // 进入文件所在目录，注意编码格式，以能够正确识别中文目录
//            ftp.changeWorkingDirectory(new String(dir.getBytes("GBK"), FTP.DEFAULT_CONTROL_ENCODING));
//
//            // 检验文件是否存在
//            InputStream is = ftp.retrieveFileStream(new String(file.getBytes("GBK"), FTP.DEFAULT_CONTROL_ENCODING));
//            if (is == null || ftp.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
//                return false;
//            }
//
//            if (is != null) {
//                is.close();
//                ftp.completePendingCommand();
//            }
//            return true;
//        } catch (Exception e) {
//            log.info("发生异常 msg={FTP查询附件状态异常！}", "FTPUtil-isFTPFileExist！", e);
//            throw new RuntimeException("FTP查询附件状态异常！" + e.getMessage());
//        } finally {
//            if (ftp != null) {
//                try {
//                    ftp.disconnect();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    /**
     * ------------------------------以下为私有方法------------------------------
     */
    // 创建文件夹
    private void mkDirs(FTPClient client, String p) throws Exception {
        if (null == p) {
            return;
        }

        if (p != null && !"".equals(p) && !"/".equals(p)) {
            String ps = "";
            for (int i = 0; i < p.split("/").length; i++) {
                ps += p.split("/")[i] + "/";
                if (!isDirExist(client, ps)) {
                    client.makeDirectory(ps);// 创建目录
                    client.changeWorkingDirectory(ps);// 进入创建的目录
                }
            }
        }
    }

    // 判断文件夹是否存在
    private static boolean isDirExist(FTPClient client, String dir) {
        boolean flag;
        try {
            flag = client.changeWorkingDirectory(dir);
        } catch (Exception e) {
            return false;
        }
        return flag;
    }

    // inputstream转byte[]
    private byte[] is2byte(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len = 1024;
        byte tmp[] = new byte[len];
        int i;
        while ((i = is.read(tmp, 0, len)) > 0) {
            baos.write(tmp, 0, i);
        }
        byte imgs[] = baos.toByteArray();
        return imgs;
    }


}
