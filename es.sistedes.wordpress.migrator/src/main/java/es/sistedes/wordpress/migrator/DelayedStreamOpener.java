package es.sistedes.wordpress.migrator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
		return url.openStream();
	}
	
	public static void setDelay(int delay) {
		DelayedStreamOpener.delay = delay;
	}
	
}
