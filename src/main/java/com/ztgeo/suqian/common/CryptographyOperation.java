package com.ztgeo.suqian.common;


import com.ztgeo.suqian.msg.CodeMsg;
import com.ztgeo.suqian.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * 加密解密、加签验签操作
 *
 * @author zoupeidong
 * @version 2018-12-11
 */
public class CryptographyOperation {
    private static Logger log = LoggerFactory.getLogger(CryptographyOperation.class);
    /**
     * 生成用于签名的公钥私钥对，返回公钥私钥字符串。
     * 不对外开放，仅用于测试
     *
     * @return 返回SignKeyPair对象，可通过get方法获取密钥字符串
     */
    public static SignKeyPair generateSignKeyPair() {
        try {
            // 生成签名密钥对
            KeyPairGenerator rsa = KeyPairGenerator.getInstance(CryptographyConstants.RSA);
            // 封装密钥对并返回预定义签名密钥对对象
            KeyPair keyPair = rsa.generateKeyPair();
            SignKeyPair signKeyPair = new SignKeyPair();
            signKeyPair.setPublickKey(new String(Base64.getEncoder().encode(keyPair.getPublic().getEncoded())));
            signKeyPair.setPrivateKey(new String(Base64.getEncoder().encode(keyPair.getPrivate().getEncoded())));
            return signKeyPair;
        } catch (ZtgeoBizRuntimeException e) {
            throw new ZtgeoBizRuntimeException(e.getCode(), e.getMessage(), e.getErrorCause());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_INTER_ERROR);
        }
    }

    /**
     * 生成数据加密对称密钥
     * 不对外开放，仅用于测试
     *
     * @return AES算法生成的对称密钥字符串
     */
    public static String generateAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(CryptographyConstants.AES);
            //初始化密钥
            String key = UUID.randomUUID().toString().replaceAll("-", "");
            keyGenerator.init(new SecureRandom(key.getBytes()));
            //生成秘钥
            SecretKey getKey = keyGenerator.generateKey();
            String resultKey = new String(Base64.getEncoder().encode(getKey.getEncoded()));
            return resultKey;
        } catch (ZtgeoBizRuntimeException e) {
            throw new ZtgeoBizRuntimeException(e.getCode(), e.getMessage(), e.getErrorCause());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_INTER_ERROR);
        }
    }

    /**
     * 生成签名，默认为UTF-8编码
     *
     * @param privateKeyStr 私钥字符串
     * @param data          待生成签名的数据
     * @return 返回签名
     */
    public static String generateSign(String privateKeyStr, String data) {
        try {
            if (StringUtils.isBlank(privateKeyStr) || StringUtils.isBlank(data))
                throw new ZtgeoBizRuntimeException(CodeMsg.SDK_SIGN_GENERATE_FAIL, "私钥或待加签数据无效，无法生成签名");
            Signature signature = Signature.getInstance(CryptographyConstants.MD5_WIEH_RSA);
            PrivateKey privateKey = getPrivateKeyFromStr(privateKeyStr);//使用私钥字符串形成私钥
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getEncoder().encode(signature.sign()));
        } catch (ZtgeoBizRuntimeException e) {
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_SIGN_GENERATE_FAIL, e.getErrorCause());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_INTER_ERROR);
        }
    }

    /**
     * 验证签名
     *
     * @param publicKeyStr 签名公钥
     * @param data         原始数据
     * @param signStr      签名字符串
     * @return 验证通过返回true，失败返回false
     */
    public static boolean signatureVerify(String publicKeyStr, String data, String signStr) {
        try {
            if (StringUtils.isBlank(publicKeyStr) || StringUtils.isBlank(data) || StringUtils.isBlank(signStr))
                throw new RuntimeException("10005-公钥或待加签数据或原始签名无效，无法验签");
            byte[] decode = Base64.getDecoder().decode(signStr); // 签名反编译
            PublicKey publicKey = getPublicKeyFromStr(publicKeyStr);
            Signature signature = Signature.getInstance(CryptographyConstants.MD5_WIEH_RSA);
            signature.initVerify(publicKey); //填充公钥
            signature.update(data.getBytes(StandardCharsets.UTF_8)); //字符集
            return signature.verify(decode);
        } catch (Exception e) {
            log.info("10002-验签失败",e);
            throw new RuntimeException("10002-验签失败");
        }
    }

    /**
     * AES加密
     *
     * @param aesKey  对称密钥
     * @param content 待加密数据
     * @return 加密后的字符串
     */
    public static String aesEncrypt(String aesKey, String content) {
        try {
            if (StringUtils.isBlank(aesKey) || StringUtils.isBlank(content))
                throw new ZtgeoBizRuntimeException(CodeMsg.SDK_ENCRYPT_FAIL, "对称密钥或待加密数据无效，无法加密");
            Cipher cipher = Cipher.getInstance(CryptographyConstants.AES_CBC_PCK_ALG);
            IvParameterSpec iv = new IvParameterSpec(initIv(CryptographyConstants.AES_CBC_PCK_ALG));
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(Base64.getDecoder().decode(aesKey.getBytes()), CryptographyConstants.AES), iv);
            byte[] encryptBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getEncoder().encode(encryptBytes));
        } catch (ZtgeoBizRuntimeException e) {
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_ENCRYPT_FAIL, e.getErrorCause());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_INTER_ERROR);
        }
    }

    /**
     * AES解密
     *
     * @param aesKey  对称密钥
     * @param content 解密的数据
     * @return 返回解密后的数据
     */
    public static String aesDecrypt(String aesKey, String content) {
        try {
            if (StringUtils.isBlank(aesKey) || StringUtils.isBlank(content))
                throw new RuntimeException("10005-对称密钥或待加密数据无效，无法解密");
            Cipher cipher = Cipher.getInstance(CryptographyConstants.AES_CBC_PCK_ALG);
            IvParameterSpec iv = new IvParameterSpec(initIv(CryptographyConstants.AES_CBC_PCK_ALG));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(aesKey.getBytes()),
                    CryptographyConstants.AES), iv);
            byte[] cleanBytes = cipher.doFinal(Base64.getDecoder().decode(content.getBytes()));
            return new String(cleanBytes, StandardCharsets.UTF_8.toString());
        } catch (ZtgeoBizRuntimeException e) {
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_DECRYPT_FAIL, e.getErrorCause());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_INTER_ERROR);
        }
    }

    /**
     * 初始向量的方法, 全部为0. 这里的写法适合于其它算法,针对AES算法的话,IV值一定是128位的(16字节).
     *
     * @param fullAlg
     * @return
     * @throws GeneralSecurityException
     */
    private static byte[] initIv(String fullAlg) {
        try {
            Cipher cipher = Cipher.getInstance(fullAlg);
            int blockSize = cipher.getBlockSize();
            byte[] iv = new byte[blockSize];
            for (int i = 0; i < blockSize; ++i) {
                iv[i] = 0;
            }
            return iv;
        } catch (Exception e) {
            int blockSize = 16;
            byte[] iv = new byte[blockSize];
            for (int i = 0; i < blockSize; ++i) {
                iv[i] = 0;
            }
            return iv;
        }
    }

    /**
     * 从字符串生成公钥
     */
    private static PublicKey getPublicKeyFromStr(String publicKeyStr) {
        try {
            //对base64位的数据进行解码
            byte[] decodeBytes = Base64.getDecoder().decode(publicKeyStr);
            //重新构造公钥
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(CryptographyConstants.RSA);
            return keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_INTER_ERROR, "生成公钥失败");
        } catch (ZtgeoBizRuntimeException e) {
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_DECRYPT_FAIL, e.getErrorCause());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_INTER_ERROR, "生成公钥失败");
        }
    }

    /**
     * 格式化私钥
     */
    private static PrivateKey getPrivateKeyFromStr(String privateKeyStr) {
        try {
            byte[] decodeBytes = Base64.getDecoder().decode(privateKeyStr);//对base64位的数据进行解码
            //重新构造私钥 默认使用PKCS8 长度1024 对于非java语言需要使用PKCS1
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodeBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(CryptographyConstants.RSA);
            return keyFactory.generatePrivate(keySpec);
        } catch (ZtgeoBizRuntimeException e) {
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_DECRYPT_FAIL, e.getErrorCause());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZtgeoBizRuntimeException(CodeMsg.SDK_DECRYPT_FAIL, "生成私钥失败");
        }
    }

}
