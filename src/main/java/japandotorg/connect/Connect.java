package japandotorg.connect;

import com.google.gson.JsonObject;
import japandotorg.Opcode;
import japandotorg.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class Connect {
    private final static String[] UNIX_TEMP_PATHS = { "XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP" };

    public static Connect open(Consumer<Packet> callback) {
        String os = System.getProperty("os.name").toLowerCase();

        // windows
        if (os.contains("win")) {
            for (int i = 0; i < 10; i++) {
                try {
                    return new WinConnection("\\\\?\\pipe\\discord-ipc-" + i, callback);
                } catch (IOException ignored) {}
            }
        }
        // unix
        else {
            String name = null;

            for (String tempPath : UNIX_TEMP_PATHS) {
                name = System.getenv(tempPath);
                if (name != null) break;
            }

            if (name == null) name = "/tmp";
            name += "/discord-ipc-";

            for (int i = 0; i < 10; i++) {
                try {
                    return new UnixConnection(name + i, callback);
                } catch (IOException ignored) {}
            }
        }

        return null;
    }

    public void write(Opcode opcode, JsonObject o) {
        o.addProperty("nonce", UUID.randomUUID().toString());

        byte[] d = o.toString().getBytes();
        ByteBuffer packet = ByteBuffer.allocate(d.length + 8);
        packet.putInt(Integer.reverseBytes(opcode.ordinal()));
        packet.putInt(Integer.reverseBytes(d.length));
        packet.put(d);

        packet.rewind();
        write(packet);
    }

    protected abstract void write(ByteBuffer buffer);

    public abstract void close();
}