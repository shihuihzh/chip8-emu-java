package com.hzh.chip8emu.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.hzh.chip8emu.Chip8Emu;
import com.hzh.chip8emu.Chip8VM;

import java.util.concurrent.TimeUnit;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
//		config.backgroundFPS = -1;
        config.foregroundFPS = 0;
		config.width = 1280;
		config.height = 640;

        Chip8VM chip8VM = new Chip8VM();
		new LwjglApplication(new Chip8Emu(config.width, chip8VM), config);

	}
}
