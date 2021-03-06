package supercoder79.cavebiomes.feature;


import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import supercoder79.cavebiomes.CaveBiomes;
import supercoder79.cavebiomes.api.CaveBiomesAPI;
import supercoder79.cavebiomes.cave.CaveDecorator;
import supercoder79.cavebiomes.layer.LayerGenerator;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class AddCaveBiomesFeature extends Feature<DefaultFeatureConfig> {
    public AddCaveBiomesFeature() {
        super(DefaultFeatureConfig.CODEC);
    }

    @Override
    public boolean generate(StructureWorldAccess world, ChunkGenerator chunkGenerator, Random random, BlockPos pos, DefaultFeatureConfig config) {
        RegistryKey<World> key = world.toServerWorld().getRegistryKey();
        if (!CaveBiomes.CONFIG.whitelistedDimensions.contains(key.getValue().toString())) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(pos);
        Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

        BitSet mask = ((ProtoChunk)chunk).getCarvingMask(GenerationStep.Carver.AIR);
        Set<BlockPos> positions = new HashSet<>();

        BlockPos.Mutable mutable = pos.mutableCopy();
        for (int x = 0; x < 16; x++) {
            mutable.setX(pos.getX() + x);
            for (int z = 0; z < 16; z++) {
                mutable.setZ(pos.getZ() + z);
                for (int y = 0; y < 256; y++) {
                    mutable.setY(y);

                    int packed = x | z << 4 | y << 8;

                    if (mask.get(packed)) {
                        if (world.getBlockState(mutable).isOf(Blocks.CAVE_AIR)) {
                            positions.add(mutable.toImmutable());
                        }
                    }
                }
            }
        }

        int threshold = CaveBiomes.CONFIG.caveLayerThreshold;

        Biome biome = world.getBiomeAccess().getBiomeForNoiseGen(chunkPos.x << 2, 0, chunkPos.z << 2);

        //regular biome based decoration
        Set<BlockPos> upperPos = positions.stream().filter(p -> p.getY() > threshold).collect(Collectors.toSet());

        Registry<Biome> biomes = world.toServerWorld().getServer().getRegistryManager().get(Registry.BIOME_KEY);
        CaveDecorator decorator = CaveBiomesAPI.getCaveDecoratorForBiome(biomes, biome);
        decorator.decorate((ChunkRegion) world, chunk, upperPos);

        //epic underground biome based decoration
        Set<BlockPos> lowerPos = positions.stream().filter(p -> p.getY() <= threshold).collect(Collectors.toSet());
        LayerGenerator.getDecorator(world.getSeed(), chunkPos.x, chunkPos.z).decorate((ChunkRegion) world, chunk, lowerPos);

        return false;
    }
}
