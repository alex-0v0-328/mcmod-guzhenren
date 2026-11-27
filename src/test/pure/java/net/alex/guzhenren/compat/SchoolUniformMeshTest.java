package net.alex.guzhenren.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 学堂制服[School Uniform]的 Epic Fight 蒙皮网格：EF 按物品 id 读 {@code animmodels/armor/<id>.json}，缺失时退回自动转换
 * （衣身只挂 Torso、下摆被大腿穿过）。坐标为 EF Blender 系（单位为格，Z 向上，−X 为左）。
 */

class SchoolUniformMeshTest {

    private static final String MESHES = "assets/guzhenren/animmodels/armor/";
    private static final String ARMATURE = "assets/epicfight/animmodels/entity/biped.json";
    private static final List<String> PIECES =
            List.of("school_uniform_chestplate", "school_uniform_leggings", "school_uniform_boots");
    private static final double PIXEL = 1.0D / 16.0D;
    private static final double HEM_FULL_Z = 10.0D * PIXEL;

    @Test
    @DisplayName("each uniform piece ships a mesh whose vertices sum to one weight over at most three biped joints")
    void everyPieceShipsAWellFormedMesh() throws IOException {
        List<String> joints = readJoints();
        for (String piece : PIECES) {
            JsonObject vertices = read(MESHES + piece + ".json").getAsJsonObject("vertices");
            int positions = vertices.getAsJsonObject("positions").get("count").getAsInt();
            int uvs = vertices.getAsJsonObject("uvs").get("count").getAsInt();
            int normals = vertices.getAsJsonObject("normals").get("count").getAsInt();
            List<Vertex> mesh = vertices(vertices);
            assertEquals(positions, mesh.size(), piece);
            for (Vertex vertex : mesh) {
                assertTrue(vertex.joints.size() <= 3, piece + " vertex " + vertex.index);
                assertEquals(1.0D, vertex.weights.stream().mapToDouble(Double::doubleValue).sum(), 1.0E-4D,
                        piece + " vertex " + vertex.index);
                vertex.joints.forEach(joint -> assertTrue(joint < joints.size(), piece + " joint " + joint));
            }
            for (var part : vertices.getAsJsonObject("parts").entrySet()) {
                JsonArray corners = part.getValue().getAsJsonObject().getAsJsonArray("array");
                assertEquals(0, corners.size() % 9, piece + " " + part.getKey() + " is made of triangles");
                for (int i = 0; i < corners.size(); i += 3) {
                    assertTrue(corners.get(i).getAsInt() < positions, piece + " " + part.getKey());
                    assertTrue(corners.get(i + 1).getAsInt() < uvs, piece + " " + part.getKey());
                    assertTrue(corners.get(i + 2).getAsInt() < normals, piece + " " + part.getKey());
                }
            }
        }
    }

    @Test
    @DisplayName("cloth more than a pixel off the middle follows only its own side's limbs")
    void eachSideFollowsItsOwnLimbs() throws IOException {
        List<String> joints = readJoints();
        for (String piece : PIECES) {
            for (Vertex vertex : vertices(read(MESHES + piece + ".json").getAsJsonObject("vertices"))) {
                String wrongSide = vertex.x < -PIXEL ? "_R" : vertex.x > PIXEL ? "_L" : null;
                if (wrongSide == null) continue;
                for (int joint : vertex.joints) {
                    assertTrue(!joints.get(joint).endsWith(wrongSide),
                            piece + " vertex " + vertex.index + " at x " + vertex.x + " hangs on " + joints.get(joint));
                }
            }
        }
    }

    @Test
    @DisplayName("below 10 px the hem moves with the thighs: each half with its own thigh, the seam half and half")
    void theHemFollowsTheThighs() throws IOException {
        List<String> joints = readJoints();
        int left = joints.indexOf("Thigh_L");
        int right = joints.indexOf("Thigh_R");
        int hem = 0;
        for (Vertex vertex : vertices(read(MESHES + "school_uniform_chestplate.json").getAsJsonObject("vertices"))) {
            if (vertex.z >= HEM_FULL_Z - 1.0E-6D) continue;
            hem++;
            if (vertex.x <= -PIXEL) {
                assertEquals(List.of(left), vertex.joints, "vertex " + vertex.index);
            } else if (vertex.x >= PIXEL) {
                assertEquals(List.of(right), vertex.joints, "vertex " + vertex.index);
            } else {
                assertEquals(2, vertex.joints.size(), "vertex " + vertex.index);
                assertTrue(vertex.joints.containsAll(List.of(left, right)), "vertex " + vertex.index);
            }
        }
        assertTrue(hem > 0, "the hem hangs below 10 px");
    }

    private record Vertex(int index, double x, double z, List<Integer> joints, List<Double> weights) {}

    private static List<Vertex> vertices(JsonObject vertices) {
        JsonArray positions = vertices.getAsJsonObject("positions").getAsJsonArray("array");
        JsonArray counts = vertices.getAsJsonObject("vcounts").getAsJsonArray("array");
        JsonArray indices = vertices.getAsJsonObject("vindices").getAsJsonArray("array");
        JsonArray weights = vertices.getAsJsonObject("weights").getAsJsonArray("array");
        List<Vertex> mesh = new ArrayList<>();
        int cursor = 0;
        for (int i = 0; i < counts.size(); i++) {
            List<Integer> joints = new ArrayList<>();
            List<Double> shares = new ArrayList<>();
            for (int k = 0; k < counts.get(i).getAsInt(); k++) {
                joints.add(indices.get(cursor++).getAsInt());
                shares.add(weights.get(indices.get(cursor++).getAsInt()).getAsDouble());
            }
            mesh.add(new Vertex(i, positions.get(i * 3).getAsDouble(), positions.get(i * 3 + 2).getAsDouble(),
                    joints, shares));
        }
        return mesh;
    }

    private static List<String> readJoints() throws IOException {
        List<String> joints = new ArrayList<>();
        read(ARMATURE).getAsJsonObject("armature").getAsJsonArray("joints")
                .forEach(joint -> joints.add(joint.getAsString()));
        return joints;
    }

    private static JsonObject read(String path) throws IOException {
        try (InputStream in = SchoolUniformMeshTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path + " ships");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
