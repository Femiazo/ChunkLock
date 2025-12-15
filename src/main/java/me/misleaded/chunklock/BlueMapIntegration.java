package me.misleaded.chunklock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;

public class BlueMapIntegration {
    private final String BORDER_ID = "chunklock-border";
    private final String COMPLETE_ID = "chunklock-complete";
    BlueMapAPI api;

    private List<BlueMapMap> maps;
    private List<Map<String, MarkerSet>> markers;

    public BlueMapIntegration() {
        // Copy icons to BlueMap web assets
        // Icon pngs: https://mc.nerothe.com/
        BlueMapAPI.onEnable(blueMapApi -> {
            api = blueMapApi;

            registerScalingScript();
            loadAssets();
            prepareMarkers();

            // temporary until we get a progression system
            setVisibility(2);
        });
    }

    private void loadAssets() {
        Path assetPath = api.getWebApp().getWebRoot().resolve("assets");
        String resourceBase = "icons/";
        try {
            Files.createDirectories(assetPath.resolve(resourceBase));
        } catch (IOException e) {
            System.out.println("ERROR: Failed to create icons folder.");
        }

        for (String item : ChunkManager.unlockables) {
            String resource = resourceBase + "minecraft_" + item.toLowerCase() + ".png";
            InputStream in = Chunklock.plugin.getResource(resource);
            try (OutputStream out = Files.newOutputStream(assetPath.resolve(resource))) {
                out.write(in.readAllBytes());
            } catch (IOException e) {
                System.out.println("ERROR: Failed to copy resource: " + resource);
                e.printStackTrace();
            }
        }
    }
    
    private void prepareMarkers() {
        maps = new ArrayList<>();
        markers = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            BlueMapWorld blueMapWorld = api.getWorld(world.getUID()).get();
            BlueMapMap map = blueMapWorld.getMaps().stream().findFirst().get();

            MarkerSet borderMS = MarkerSet.builder().label("Items (Border)").sorting(1).build();
            MarkerSet completeMS = MarkerSet.builder().label("Items (Complete)").sorting(2).build();

            maps.add(map);
            markers.add(Map.of(BORDER_ID, borderMS, COMPLETE_ID, completeMS));
        }

        for (List<Integer> chunk : ChunkManager.getChunks()) {
            int x = chunk.get(0);
            int z = chunk.get(1);
            int w = chunk.get(2);
            int y = Bukkit.getWorlds().get(w).getMaxHeight();
            Map<String, MarkerSet> sets = markers.get(w);

            String item = ChunkManager.unlockMaterial(chunk);
            String displayHTML = """
                <div style="font-weight: bold; padding-bottom: 10px; white-space: nowrap;">
                Chunk (%d, %d)
                </div>
                <span style="white-space: nowrap;">%s</span>
            """.formatted(x, z, WordUtils.capitalizeFully(item.replace('_', ' ')));

            POIMarker marker = POIMarker.builder().position(16*x + 8.0, y, 16*z + 8.0)
                                        .label("Chunk (" + x + ", " + z + ")")
                                        .detail(displayHTML)
                                        .icon("assets/icons/minecraft_" + item.toLowerCase() + ".png", 31, 31)
                                        .styleClasses("fixed-on-map")
                                        .build();

            if (ChunkManager.isBorder(chunk)) sets.get(BORDER_ID).put(x + "-" + z, marker);
            if (!ChunkManager.isUnlocked(chunk)) sets.get(COMPLETE_ID).put(x + "-" + z, marker);
        }
    }

    public void setVisibility(int visibility) {
        if (api == null) return;

        for (int w = 0; w < maps.size(); w++ ) {
            Map<String, MarkerSet> sets = maps.get(w).getMarkerSets();
            Map<String, MarkerSet> itemSets = markers.get(w);

            switch (visibility) {
                case 2:
                    sets.put(COMPLETE_ID, itemSets.get(COMPLETE_ID));
                case 1:
                    sets.put(BORDER_ID, itemSets.get(BORDER_ID));
                    break;
                case 0:
                    sets.remove(COMPLETE_ID);
                    sets.remove(BORDER_ID);
            }
        }
    }

    private void registerScalingScript() {
        String style = """
            .fixed-on-map {
                transform-origin: top left;
                transform: scale(var(--marker-scale, 1));
            }
        """;

        // credit to https://github.com/mataaj/bluemap-fixed-2d-html-markers
        String script = """
            const config = {
                pixelToBlockRatio: 0.125,
                idealHeightTodistanceRatio: 0.651613,
            };

            const camera = bluemap.mapViewer.camera

            function update2dHtmlMarkers() {
                if (camera.ortho === 0) {
                    document.documentElement.style.removeProperty("--marker-scale");
                    return;
                }

                const expectedDistance =
                    config.idealHeightTodistanceRatio *
                    window.innerHeight *
                    config.pixelToBlockRatio;

                const scale = expectedDistance / camera.distance;
                document.documentElement.style.setProperty("--marker-scale", scale);
            }

            (function loop() {
                update2dHtmlMarkers();
                requestAnimationFrame(loop);
            })();
        """;

        Path assetPath = api.getWebApp().getWebRoot().resolve("assets");
        Path stylePath = assetPath.resolve("fixedHtml2dMarkers.css");
        Path scriptPath = assetPath.resolve("fixedHtml2dMarkers.js");

        try {
            Files.write(stylePath, style.getBytes());
            Files.write(scriptPath, script.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        api.getWebApp().registerStyle("assets/fixedHtml2dMarkers.css");
        api.getWebApp().registerScript("assets/fixedHtml2dMarkers.js");
    }
}