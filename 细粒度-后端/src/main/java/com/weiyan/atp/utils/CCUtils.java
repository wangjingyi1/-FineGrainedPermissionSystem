package com.weiyan.atp.utils;

import com.weiyan.atp.constant.BaseException;
import com.weiyan.atp.data.bean.ChaincodeResponse;
import com.weiyan.atp.data.bean.ChaincodeResponse.Status;
import com.weiyan.atp.data.bean.DABEUser;
import com.weiyan.atp.data.bean.intergration.Cert;
import com.weiyan.atp.data.request.chaincode.plat.BaseCCRequest;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

/**
 * @author : 魏延thor
 * @since : 2020/6/1
 */
@Slf4j
public class CCUtils {
    private CCUtils() {
    }
    private static final String HEX = "0123456789abcdef";

    public static <T extends BaseCCRequest> void sign(T t, String priKey) {
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            // 把 私钥的Base64文本 转换为已编码的 私钥bytes
            byte[] encPriKey = Base64.decode(priKey);
            // 创建 已编码的私钥规格
            PKCS8EncodedKeySpec encPriKeySpec = new PKCS8EncodedKeySpec(encPriKey);
            // 获取指定算法的密钥工厂, 根据 已编码的私钥规格, 生成私钥对象
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(encPriKeySpec);
            String jsonString = JsonProviderHolder.JACKSON.toJsonString(t);

            Signature signer = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
            signer.initSign(privateKey);
            signer.update(jsonString.getBytes());
            byte[] sign = signer.sign();
            t.setSign(bytes2hex(sign));
        } catch (Exception e) {
            log.error("Error signing the data with private key", e);
            throw new BaseException("Error signing the data with private key", e);
        }
    }

    public static void saveResponse2(String userPath, String fileName, ChaincodeResponse response) {
        if (response.getStatus() == Status.FAIL) {
            log.warn("query chaincode error: {}", response.getMessage());
        }
        try {
            String filePath = userPath + fileName;
            String resource = response.getMessage();
            Cert cert = JsonProviderHolder.JACKSON.parse(resource, Cert.class);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.MONTH,1);
            cert.setExpireDate(calendar.getTime().toString());
            String jsonString = JsonProviderHolder.JACKSON.toJsonString(cert);
            saveDABEUser(filePath, jsonString);
        } catch (Exception e) {
            log.warn("create user error", e);
        }
    }

    public static DABEUser saveResponse(String userPath, String fileName, ChaincodeResponse response) {
        if (response.getStatus() == Status.FAIL) {
            log.warn("query chaincode error: {}", response.getMessage());
            return null;
        }
        try {
            String filePath = userPath + fileName;
            String resource = response.getMessage();
            DABEUser newUser = JsonProviderHolder.JACKSON.parse(resource, DABEUser.class);
            saveDABEUser(filePath, resource);
            return newUser;
        } catch (Exception e) {
            log.warn("create user error", e);
            return null;
        }
    }

    public static void saveDABEUser(String filePath, String resource) {
        try {
            File file = new File(filePath);
            FileUtils.touch(file);
            IOUtils.write(resource, new FileOutputStream(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BaseException("io error for " + filePath);
        }
    }

    private static String bytes2hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));
        }
        return sb.toString();
    }
}
