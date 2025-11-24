package io.github.Elevador.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import io.github.Elevador.GameManager;

/** Launches the desktop (LWJGL3) application. */
public class  DesktopLauncher {
	 public static void main(String[] arg) {
	        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();	     
	        config.setTitle("Elevador Multitread - Grupo Albram, Gabriel, João, Marcelo");        
	        config.setWindowedMode(800, 600);	        
	        config.setForegroundFPS(60);	       
	        config.useVsync(true);
	        new Lwjgl3Application(new GameManager(), config);
	    }
}