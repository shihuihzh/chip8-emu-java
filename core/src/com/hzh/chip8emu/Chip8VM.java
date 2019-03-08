package com.hzh.chip8emu;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static com.hzh.chip8emu.Const.*;

public class Chip8VM {

    private byte delayTimer;
    private byte soundTimer;
    private boolean drawFlag;

    private short opcode;
    private short I;
    private short pc;
    private short sp;

    private byte[] V;
    private byte[] gfx;
    private byte[] key;
    private byte[] memory;

    private short[] stack;


    public void load(byte[] rom) {

        // init data
        // 0x200 = 512, rom start from here
        this.pc = START_ADDRESS;
        this.opcode = 0;
        this.I = 0;
        this.sp = 0;

        this.V = new byte[REGISTER_COUNT];
        this.gfx = new byte[V_RAM_SIZE];
        this.key = new byte[KEY_COUNT];
        this.memory = new byte[RAM_SIZE];
        this.stack = new short[STACK_SIZE];

        this.delayTimer = 0;
        this.soundTimer = 0;

        // load font
        System.arraycopy(CHIP8_FONTSET, 0, memory, 0, CHIP8_FONTSET.length);

        // load rom
        System.arraycopy(rom, 0, memory, START_ADDRESS, rom.length);
    }

    public void cycle() {
        // Fetch opcode
        opcode = (short) (memory[pc] << 8 | (memory[pc + 1] & 0xff));

        // Process opcode
        switch(opcode & 0xF000)
        {
            case 0x0000:
                switch(opcode & 0x000F)
                {
                    case 0x0000: // 0x00E0: Clears the screen
                        Arrays.fill(gfx, (byte) 0);
                        drawFlag = true;
                        pc += 2;
                        break;

                    case 0x000E: // 0x00EE: Returns from subroutine
                        pc = stack[--sp];	// 16 levels of stack, decrease stack pointer to prevent overwrite and Put the stored return address from the stack back into the program counter
                        pc += 2;		// Don't forget to increase the program counter!
                        break;

                    default:
                        System.out.println(String.format("Unknown opcode [0x0000]: 0x%X", opcode));
                }
                break;

            case 0x1000: // 0x1NNN: Jumps to address NNN
                pc = (short) (opcode & 0x0FFF);
                break;

            case 0x2000: // 0x2NNN: Calls subroutine at NNN.
                stack[sp++] = pc;			// Store current address in stack and increment stack pointer
                pc = (short) (opcode & 0x0FFF);	// Set the program counter to the address at NNN
                break;

            case 0x3000: // 0x3XNN: Skips the next instruction if VX equals NN
                if(V[(opcode & 0x0F00) >> 8] == (opcode & 0x00FF))
                    pc += 4;
                else
                    pc += 2;
                break;

            case 0x4000: // 0x4XNN: Skips the next instruction if VX doesn't equal NN
                if(V[(opcode & 0x0F00) >> 8] != (opcode & 0x00FF))
                    pc += 4;
                else
                    pc += 2;
                break;

            case 0x5000: // 0x5XY0: Skips the next instruction if VX equals VY.
                if(V[(opcode & 0x0F00) >> 8] == V[(opcode & 0x00F0) >> 4])
                    pc += 4;
                else
                    pc += 2;
                break;

            case 0x6000: // 0x6XNN: Sets VX to NN.
                V[(opcode & 0x0F00) >> 8] = (byte) (opcode & 0x00FF);
                pc += 2;
                break;

            case 0x7000: // 0x7XNN: Adds NN to VX.
                V[(opcode & 0x0F00) >> 8] += opcode & 0x00FF;
                pc += 2;
                break;

            case 0x8000:
                switch(opcode & 0x000F)
                {
                    case 0x0000: // 0x8XY0: Sets VX to the value of VY
                        V[(opcode & 0x0F00) >> 8] = V[(opcode & 0x00F0) >> 4];
                        pc += 2;
                        break;

                    case 0x0001: // 0x8XY1: Sets VX to "VX OR VY"
                        V[(opcode & 0x0F00) >> 8] |= V[(opcode & 0x00F0) >> 4];
                        pc += 2;
                        break;

                    case 0x0002: // 0x8XY2: Sets VX to "VX AND VY"
                        V[(opcode & 0x0F00) >> 8] &= V[(opcode & 0x00F0) >> 4];
                        pc += 2;
                        break;

                    case 0x0003: // 0x8XY3: Sets VX to "VX XOR VY"
                        V[(opcode & 0x0F00) >> 8] ^= V[(opcode & 0x00F0) >> 4];
                        pc += 2;
                        break;

                    case 0x0004: // 0x8XY4: Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't					
                        if(V[(opcode & 0x00F0) >> 4] > (0xFF - V[(opcode & 0x0F00) >> 8]))
                            V[0xF] = 1; //carry
                        else
                            V[0xF] = 0;
                        V[(opcode & 0x0F00) >> 8] += V[(opcode & 0x00F0) >> 4];
                        pc += 2;
                        break;

                    case 0x0005: // 0x8XY5: VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                        if(V[(opcode & 0x00F0) >> 4] > V[(opcode & 0x0F00) >> 8])
                            V[0xF] = 0; // there is a borrow
                        else
                            V[0xF] = 1;
                        V[(opcode & 0x0F00) >> 8] -= V[(opcode & 0x00F0) >> 4];
                        pc += 2;
                        break;

                    case 0x0006: // 0x8XY6: Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
                        V[0xF] = (byte) (V[(opcode & 0x0F00) >> 8] & 0x1);
                        V[(opcode & 0x0F00) >> 8] >>= 1;
                        pc += 2;
                        break;

                    case 0x0007: // 0x8XY7: Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                        if(V[(opcode & 0x0F00) >> 8] > V[(opcode & 0x00F0) >> 4])	// VY-VX
                            V[0xF] = 0; // there is a borrow
                        else
                            V[0xF] = 1;
                        V[(opcode & 0x0F00) >> 8] = (byte) (V[(opcode & 0x00F0) >> 4] - V[(opcode & 0x0F00) >> 8]);
                        pc += 2;
                        break;

                    case 0x000E: // 0x8XYE: Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
                        V[0xF] = (byte) (V[(opcode & 0x0F00) >> 8] >> 7);
                        V[(opcode & 0x0F00) >> 8] <<= 1;
                        pc += 2;
                        break;

                    default:
                        System.out.println(String.format("Unknown opcode [0x8000]: 0x%X", opcode));
                }
                break;

            case 0x9000: // 0x9XY0: Skips the next instruction if VX doesn't equal VY
                if(V[(opcode & 0x0F00) >> 8] != V[(opcode & 0x00F0) >> 4])
                    pc += 4;
                else
                    pc += 2;
                break;

            case 0xA000: // ANNN: Sets I to the address NNN
                I = (short) (opcode & 0x0FFF);
                pc += 2;
                break;

            case 0xB000: // BNNN: Jumps to the address NNN plus V0
                pc = (short) ((opcode & 0x0FFF) + V[0]);
                break;

            case 0xC000: // CXNN: Sets VX to a random number and NN
                V[(opcode & 0x0F00) >> 8] = (byte) ((ThreadLocalRandom.current().nextInt() % 0xFF) & (opcode & 0x00FF));
                pc += 2;
                break;

            case 0xD000: // DXYN: Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels. 
                // Each row of 8 pixels is read as bit-coded starting from memory location I; 
                // I value doesn't change after the execution of this instruction. 
                // VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn, 
                // and to 0 if that doesn't happen
            {
                short x = V[(opcode & 0x0F00) >> 8];
                short y = V[(opcode & 0x00F0) >> 4];
                short height = (short) (opcode & 0x000F);
                short pixel;

                V[0xF] = 0;
                for (int yline = 0; yline < height; yline++)
                {
                    pixel = memory[I + yline];
                    for(int xline = 0; xline < 8; xline++)
                    {
                        if((pixel & (0x80 >> xline)) != 0)
                        {
                            if(gfx[(x + xline + ((y + yline) * 64))] == 1)
                            {
                                V[0xF] = 1;
                            }
                            gfx[x + xline + ((y + yline) * 64)] ^= 1;
                        }
                    }
                }

                drawFlag = true;
                pc += 2;
            }
            break;

            case 0xE000:
                switch(opcode & 0x00FF)
                {
                    case 0x009E: // EX9E: Skips the next instruction if the key stored in VX is pressed
                        if(key[V[(opcode & 0x0F00) >> 8]] != 0)
                            pc += 4;
                        else
                            pc += 2;
                        break;

                    case 0x00A1: // EXA1: Skips the next instruction if the key stored in VX isn't pressed
                        if(key[V[(opcode & 0x0F00) >> 8]] == 0)
                            pc += 4;
                        else
                            pc += 2;
                        break;

                    default:
                        System.out.println(String.format("Unknown opcode [0xE000]: 0x%X", opcode));
                }
                break;

            case 0xF000:
                switch(opcode & 0x00FF)
                {
                    case 0x0007: // FX07: Sets VX to the value of the delay timer
                        V[(opcode & 0x0F00) >> 8] = delayTimer;
                        pc += 2;
                        break;

                    case 0x000A: // FX0A: A key press is awaited, and then stored in VX		
                    {
                        boolean keyPress = false;

                        for(int i = 0; i < 16; ++i)
                        {
                            if(key[i] != 0)
                            {
                                V[(opcode & 0x0F00) >> 8] = (byte) i;
                                keyPress = true;
                            }
                        }

                        // If we didn't received a keypress, skip this cycle and try again.
                        if(!keyPress)
                            return;

                        pc += 2;
                    }
                    break;

                    case 0x0015: // FX15: Sets the delay timer to VX
                        delayTimer = V[(opcode & 0x0F00) >> 8];
                        pc += 2;
                        break;

                    case 0x0018: // FX18: Sets the sound timer to VX
                        soundTimer = V[(opcode & 0x0F00) >> 8];
                        pc += 2;
                        break;

                    case 0x001E: // FX1E: Adds VX to I
                        if(I + V[(opcode & 0x0F00) >> 8] > 0xFFF)	// VF is set to 1 when range overflow (I+VX>0xFFF), and 0 when there isn't.
                            V[0xF] = 1;
                        else
                            V[0xF] = 0;
                        I += V[(opcode & 0x0F00) >> 8];
                        pc += 2;
                        break;

                    case 0x0029: // FX29: Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are represented by a 4x5 font
                        I = (short) (V[(opcode & 0x0F00) >> 8] * 0x5);
                        pc += 2;
                        break;

                    case 0x0033: // FX33: Stores the Binary-coded decimal representation of VX at the addresses I, I plus 1, and I plus 2
                        memory[I]     = (byte) (V[(opcode & 0x0F00) >> 8] / 100);
                        memory[I + 1] = (byte) ((V[(opcode & 0x0F00) >> 8] / 10) % 10);
                        memory[I + 2] = (byte) ((V[(opcode & 0x0F00) >> 8] % 100) % 10);
                        pc += 2;
                        break;

                    case 0x0055: // FX55: Stores V0 to VX in memory starting at address I					
                        System.arraycopy(V, 0, memory, I, ((opcode & 0x0F00) >> 8));
                        // On the original interpreter, when the operation is done, I = I + X + 1.
                        I += ((opcode & 0x0F00) >> 8) + 1;
                        pc += 2;
                        break;

                    case 0x0065: // FX65: Fills V0 to VX with values from memory starting at address I					
                        for (int i = 0; i <= ((opcode & 0x0F00) >> 8); ++i)
                            V[i] = memory[I + i];

                        System.arraycopy(memory, I + 1, V, 0, ((opcode & 0x0F00) >> 8));
                        // On the original interpreter, when the operation is done, I = I + X + 1.
                        I += ((opcode & 0x0F00) >> 8) + 1;
                        pc += 2;
                        break;

                    default:
                        System.out.println(String.format("Unknown opcode [0xF000]: 0x%X", opcode));
                }
                break;

            default:
                System.out.println(String.format("Unknown opcode: 0x%X", opcode));
        }

        // Update timers
        if(delayTimer > 0)
            --delayTimer;

        if(soundTimer > 0)
        {
            if(soundTimer == 1)
                System.out.println("BEEP!");
            --soundTimer;
        }
    }

    public boolean isDrawFlag() {
        return drawFlag;
    }


    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getGfx() {
        return gfx;
    }
}
