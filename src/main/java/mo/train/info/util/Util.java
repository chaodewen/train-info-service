package mo.train.info.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Util {
	/**
	 * 存储当天的公开站名与内部站名对应关系
	 */
	public static boolean saveStationName() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String fileName = "stationName_" + df.format(Calendar.getInstance().getTime()).toString();
		if(new File(fileName).exists()) {
			return true;
		}
		else {
			String url = "https://kyfw.12306.cn/otn/resources/js/framework/station_name.js";
			if(Util.sendHTTPGet(url, fileName) != null) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	/**
     * 存储今天起60天内车次对应的trainNo
     */
	public static boolean saveTrainList() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");	
		String fileName = df.format(Calendar.getInstance().getTime()).toString();
		if(new File(fileName).exists()) {
			return true;
		}
		else {
			// 此处添加删除无效文件的语句
			String url = "https://kyfw.12306.cn/otn/resources/js/query/train_list.js";
			if(Util.sendHTTPGet(url, fileName) != null) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	private static HostnameVerifier ignoreHostnameVerifier = new HostnameVerifier() {
        public boolean verify(String s, SSLSession sslsession) {
//            System.out.println("WARNING: Hostname is not matched for cert.");
            return true;
        }
    };
    /**
     * Ignore Certification
     */
    private static TrustManager ignoreCertificationTrustManger = new X509TrustManager() {
        private X509Certificate[] certificates;
        @Override
        public void checkClientTrusted(X509Certificate certificates[],
                String authType) throws CertificateException {
            if (this.certificates == null) {
                this.certificates = certificates;
//                System.out.println("init at checkClientTrusted");
            }
        }
        @Override
        public void checkServerTrusted(X509Certificate[] ax509certificate,
                String s) throws CertificateException {
            if (this.certificates == null) {
                this.certificates = ax509certificate;
//                System.out.println("init at checkServerTrusted");
            }
        }
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };
    /**
     * 发送HTTPS的GET请求
     * @param urlString
     * @param filePath
     * @return null出错 其余正常返回
     */
    public static String sendHTTPGet(String urlString, String filePath) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);
        try {
            URL url = new URL(urlString);
            /*
             * use ignore host name verifier
             */
            HttpsURLConnection.setDefaultHostnameVerifier(ignoreHostnameVerifier);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // Prepare SSL Context
            TrustManager[] tm = { ignoreCertificationTrustManger };
            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
            sslContext.init(null, tm, new java.security.SecureRandom());

            SSLSocketFactory ssf = sslContext.getSocketFactory();
            connection.setSSLSocketFactory(ssf);
            InputStream reader = connection.getInputStream();
            byte[] bytes = new byte[512];
            
            int length = reader.read(bytes);
            
            do {
                buffer.write(bytes, 0, length);
                length = reader.read(bytes);
            } while (length > 0);
            reader.close();
            connection.disconnect();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        // 当filePath为空时返回字符串 否则写文件返回描述
        if(filePath == null) {
    		try {
    			return new String(buffer.toByteArray(), "utf-8");
    		} catch (UnsupportedEncodingException e) {
    			e.printStackTrace();
    			return null;
    		}
        }
        else {
        	File file = new File(filePath);
        	FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
				buffer.writeTo(fos);
				return "success";
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("要写入文件未找到！");
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("写入文件错误！");
				return null;
			}
        }
    }
    public static String sendHTTPGet(String urlString) {
    	return sendHTTPGet(urlString, null);
    }
}