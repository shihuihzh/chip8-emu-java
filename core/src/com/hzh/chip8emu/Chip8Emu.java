package com.hzh.chip8emu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import static com.hzh.chip8emu.Const.*;

public class Chip8Emu extends ApplicationAdapter {

    private float width;
    private float height;
    private int pixelSize;

    private byte[] vRAM;
    private Chip8VM vm;

    private Texture img;
    private Sprite sprite;
    private SpriteBatch batch;
    private int fps;
    private long lastTime;

    public Chip8Emu(float width, Chip8VM vm) {
        this.width = width;
        this.height = width / RES_WIDTH * RES_HEIGHT;
        this.pixelSize = (int) (width / RES_WIDTH);
        this.vm = vm;
    }

    @Override
    public void create() {
        img = new Texture(PIXEL_PNG);
        batch = new SpriteBatch();
        sprite = new Sprite(img, 0, 0, img.getWidth() * this.pixelSize, img.getHeight() * this.pixelSize);
        startEmu(Gdx.files.internal("tetris.c8").readBytes());

        lastTime = System.currentTimeMillis();
    }

    private void startEmu(byte[] rom) {
        vm.load(rom);
        this.vRAM = vm.getGfx();
    }

    @Override
    public void render() {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // run next
        vm.cycle();
//        vm.cycle();
//        vm.cycle();
//        vm.cycle();
//        vm.cycle();

        batch.begin();
        for (int i = 0; i < this.vRAM.length; i++) {
            if (this.vRAM[i] == 1) {
                float x = i % RES_WIDTH;
                float y = RES_HEIGHT - 1 - (i / RES_WIDTH);
                sprite.setPosition(x * pixelSize, y * pixelSize);
                sprite.draw(batch);
            }
        }
        batch.end();

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
        batch.dispose();
        img.dispose();
    }

}
