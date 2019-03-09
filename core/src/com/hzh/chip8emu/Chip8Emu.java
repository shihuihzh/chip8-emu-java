package com.hzh.chip8emu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.hzh.chip8emu.Const.*;

public class Chip8Emu extends ApplicationAdapter implements InputProcessor  {

    private float width;
    private float height;
    private int pixelSize;

    private byte[] vRAM;
    private byte[] key;
    private Chip8VM vm;

    private int fps;
    private long lastTime;
    private static Sound beep;
    private ShapeRenderer shapeRenderer;
    private  Map<Integer, Integer> keyMap;

    public Chip8Emu(float width, Chip8VM vm) {
        this.width = width;
        this.height = width / RES_WIDTH * RES_HEIGHT;
        this.pixelSize = (int) (width / RES_WIDTH);
        this.vm = vm;
    }

    public static void beep() {
        beep.play(1.0f);
    }

    @Override
    public void create() {
        intKeyMap();
        Gdx.input.setInputProcessor(this);
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setColor(Color.WHITE);

        beep = Gdx.audio.newSound(Gdx.files.internal("beep.mp3"));
        startEmu(Gdx.files.internal("roms/games/Pong 2 (Pong hack) [David Winter, 1997].ch8").readBytes());
        lastTime = System.currentTimeMillis();
    }

    private void intKeyMap() {
        keyMap = new TreeMap<>();
        keyMap.put(Input.Keys.NUM_1, 0x1);
        keyMap.put(Input.Keys.NUM_2, 0x2);
        keyMap.put(Input.Keys.NUM_3, 0x3);
        keyMap.put(Input.Keys.NUM_4, 0xC);
        keyMap.put(Input.Keys.Q, 0x4);
        keyMap.put(Input.Keys.W, 0x5);
        keyMap.put(Input.Keys.E, 0x6);
        keyMap.put(Input.Keys.R, 0xD);
        keyMap.put(Input.Keys.A, 0x7);
        keyMap.put(Input.Keys.S, 0x8);
        keyMap.put(Input.Keys.D, 0x9);
        keyMap.put(Input.Keys.F, 0xE);
        keyMap.put(Input.Keys.Z, 0xA);
        keyMap.put(Input.Keys.X, 0x0);
        keyMap.put(Input.Keys.C, 0xB);
        keyMap.put(Input.Keys.V, 0xF);
    }

    private void startEmu(byte[] rom) {
        vm.load(rom);
        this.vRAM = vm.getGfx();
        this.key = vm.getKey();
    }

    @Override
    public void render() {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // run next
        vm.cycle();
        vm.cycle();
        vm.cycle();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < this.vRAM.length; i++) {
            if (this.vRAM[i] == 1) {
                float x = i % RES_WIDTH;
                float y = RES_HEIGHT - 1 - (i / RES_WIDTH);
                shapeRenderer.rect(x * pixelSize, y * pixelSize, pixelSize, pixelSize);
            }
        }
        shapeRenderer.end();

        fps++;
        long newTime = System.currentTimeMillis();
        if(newTime - lastTime >= 1000) {
            lastTime = newTime;
            System.out.println(fps);
            fps = 0;
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if(keyMap.containsKey(keycode)) {
            this.key[keyMap.get(keycode)] = 1;
            System.out.println("key down: " + keycode);
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if(keyMap.containsKey(keycode)) {
            this.key[keyMap.get(keycode)] = 0;
            System.out.println("key up: " + keycode);
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
