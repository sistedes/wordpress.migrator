package es.sistedes.wordpress.migrator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public final class DelayedStreamOpener {

	private static int delay = 0;
	
	private static long lastRequest = Long.MIN_VALUE;
	
	public static InputStream open(URL url) throws IOException {
		long now = System.currentTimeMillis();
		if (lastRequest + delay > now) {
			try {
				Thread.sleep(delay - (now  - lastRequest));
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		lastRequest = System.currentTimeMillis();
		URLConnection connection = url.openConnection();
		if (connection instanceof HttpsURLConnection) {
			HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
			httpsConnection.setHostnameVerifier(new HostnameVerifier() {
				// Quick and dirty way to ignore bad DNS name in SSL certificates
				// Used to migrate JCIS and PROLE 2016, which were lost in biblioteca.sistedes.es,
				// but we kept a copy in bdsdev.dsic.upv.es 
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
		}
		return connection.getInputStream();
	}
	
	public static void setDelay(int delay) {
		DelayedStreamOpener.delay = delay;
	}
	
}
