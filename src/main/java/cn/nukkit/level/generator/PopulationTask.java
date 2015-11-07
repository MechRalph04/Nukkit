package cn.nukkit.level.generator;

import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.SimpleChunkManager;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.scheduler.AsyncTask;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PopulationTask extends AsyncTask {
    public boolean state;
    public int levelId;
    public BaseFullChunk chunk;

    public BaseFullChunk[] chunks = new BaseFullChunk[9];

    public PopulationTask(Level level, BaseFullChunk chunk) {
        this.state = true;
        this.levelId = level.getId();
        this.chunk = chunk;

        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                continue;
            }
            int xx = -1 + i % 3;
            int zz = -1 + (i / 3);
            BaseFullChunk ck = level.getChunk(chunk.getX() + xx, chunk.getZ() + zz, false);
            this.chunks[i] = ck;
        }
    }

    @Override
    public void onRun() {
        SimpleChunkManager manager = (SimpleChunkManager) this.getFromThreadStore("generation.level" + this.levelId + ".manager");

        Generator generator = (Generator) this.getFromThreadStore("generation.level" + this.levelId + ".generator");

        if (manager == null || generator == null) {
            this.state = false;
            return;
        }

        BaseFullChunk[] chunks = new BaseFullChunk[9];

        BaseFullChunk chunk = this.chunk.clone();

        if (chunk == null) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                continue;
            }

            int xx = -1 + i % 3;
            int zz = -1 + (i / 3);
            BaseFullChunk ck = this.chunks[i];

            if (ck == null) {
                try {
                    chunks[i] = (BaseFullChunk) this.chunk.getClass().getMethod("getEmptyChunk", int.class, int.class).invoke(null, chunk.getX() + xx, chunk.getZ() + zz);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                chunks[i] = ck;
            }
        }

        manager.setChunk(chunk.getX(), chunk.getZ(), chunk);
        if (!chunk.isGenerated()) {
            generator.generateChunk(chunk.getX(), chunk.getZ());
            chunk.setGenerated();
        }

        for (BaseFullChunk c : chunks) {
            if (c != null) {
                manager.setChunk(c.getX(), c.getZ(), c);
                if (!c.isGenerated()) {
                    generator.generateChunk(c.getX(), c.getZ());
                    c = manager.getChunk(c.getX(), c.getZ());
                    c.setGenerated();
                }
            }
        }

        generator.populateChunk(chunk.getX(), chunk.getZ());

        chunk = manager.getChunk(chunk.getX(), chunk.getZ());
        chunk.recalculateHeightMap();
        chunk.populateSkyLight();
        chunk.setLightPopulated();
        chunk.setPopulated();
        this.chunk = chunk;

        manager.setChunk(chunk.getX(), chunk.getZ(), null);

        for (int i = 0; i < chunks.length; i++) {
            BaseFullChunk c = chunks[i];
            if (c != null) {
                //c = chunks[i] = manager.getChunk(c.getX(), c.getZ());
                if (!c.hasChanged()) {
                    chunks[i] = null;
                }
            }
        }

        manager.cleanChunks();

        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                continue;
            }

            this.chunks[i] = chunks[i];
        }
    }

    @Override
    public void onCompletion(Server server) {
        Level level = server.getLevel(this.levelId);
        if (level != null) {
            if (!this.state) {
                level.registerGenerator();
                return;
            }

            BaseFullChunk chunk = this.chunk.clone();
            if (chunk == null) {
                return;
            }

            for (int i = 0; i < 9; i++) {
                if (i == 4) {
                    continue;
                }

                BaseFullChunk c = this.chunks[i];
                if (c != null) {
                    level.generateChunkCallback(c.getX(), c.getZ(), c);
                }
            }

            level.generateChunkCallback(chunk.getX(), chunk.getZ(), chunk);
        }
    }
}
