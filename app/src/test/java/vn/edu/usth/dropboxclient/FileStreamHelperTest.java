
package vn.edu.usth.dropboxclient;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import vn.edu.usth.dropboxclient.ui.transfer.FileStreamHelper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FileStreamHelperTest {

    // Test 1: Verify text file content
    @Test
    public void copyTextFile_shouldPreserveContent()
            throws IOException {

        byte[] original =
                "Hello Dropbox!".getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream input =
                new ByteArrayInputStream(original);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        long copiedBytes = FileStreamHelper.copy(input, output);

        assertEquals((long) original.length, copiedBytes);
        assertArrayEquals(original, output.toByteArray());
    }

    // Test 2: Verify binary data integrity
    @Test
    public void copyBinaryFile_shouldPreserveBytes()
            throws IOException {

        byte[] original = new byte[20000];

        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (i % 256);
        }

        ByteArrayInputStream input =
                new ByteArrayInputStream(original);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        long copiedBytes = FileStreamHelper.copy(input, output);

        assertEquals(20000L, copiedBytes);
        assertArrayEquals(original, output.toByteArray());
    }

    // Test 3: Verify empty file handling
    @Test
    public void copyEmptyFile_shouldReturnZero()
            throws IOException {

        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[0]);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        long copiedBytes = FileStreamHelper.copy(input, output);

        assertEquals(0L, copiedBytes);
        assertEquals(0, output.size());
    }

    // Test 4: Simulate InputStream read failure
    @Test
    public void copyFile_whenReadFails_shouldThrowIOException() {

        InputStream failingInput = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("Simulated read failure");
            }

            @Override
            public int read(byte[] buffer, int offset, int length)
                    throws IOException {
                throw new IOException("Simulated read failure");
            }
        };

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        try {
            FileStreamHelper.copy(failingInput, output);

            fail("Expected IOException was not thrown");

        } catch (IOException e) {
            assertEquals(
                    "Simulated read failure",
                    e.getMessage()
            );
        }
    }

    // Test 5: Simulate OutputStream write failure
    @Test
    public void copyFile_whenWriteFails_shouldThrowIOException() {

        byte[] original =
                "Hello Dropbox!".getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream input =
                new ByteArrayInputStream(original);

        OutputStream failingOutput = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                throw new IOException("Simulated write failure");
            }

            @Override
            public void write(byte[] buffer, int offset, int length)
                    throws IOException {
                throw new IOException("Simulated write failure");
            }
        };

        try {
            FileStreamHelper.copy(input, failingOutput);

            fail("Expected IOException was not thrown");

        } catch (IOException e) {
            assertEquals(
                    "Simulated write failure",
                    e.getMessage()
            );
        }
    }
}
