package solid;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A CArtAgO artifact that agent can use to interact with LDP containers in a Solid pod.
 */
public class Pod extends Artifact {

    private String podURL; // the location of the Solid pod 

    /**
     * Method called by CArtAgO to initialize the artifact. 
     *
     * @param podURL The location of a Solid pod
     */
    public void init(String podURL) {
        if (!podURL.endsWith("/")) {
            podURL = podURL + "/";
        }
        this.podURL = podURL;
        log("Pod artifact initialized for: " + this.podURL);
    }

    /**
     * CArtAgO operation for creating a Linked Data Platform container in the Solid pod
     *
     * @param containerName The name of the container to be created
     * 
     */
    @OPERATION
    public void createContainer(String containerName) {
        String containerURL = podURL + containerName + "/";
        try {
            // Check if container exists using a HEAD request.
            URL url = new URL(containerURL);
            HttpURLConnection headCon = (HttpURLConnection) url.openConnection();
            headCon.setRequestMethod("HEAD");
            int responseCode = headCon.getResponseCode();
            if(responseCode == 200) {
                log("Container already exists: " + containerURL);
                return;
            }
        } catch(Exception e) {
            // An exception here suggests the container might not exist.
            log("Container not found, proceeding to create: " + containerURL);
        }
        try {
            URL url = new URL(containerURL);
            HttpURLConnection putCon = (HttpURLConnection) url.openConnection();
            putCon.setRequestMethod("PUT");
            putCon.setDoOutput(true);
            // For a container, we create an empty Turtle file.
            putCon.setRequestProperty("Content-Type", "text/turtle");
            OutputStream os = putCon.getOutputStream();
            os.write("".getBytes("UTF-8"));
            os.flush();
            os.close();
            int putResponse = putCon.getResponseCode();
            if(putResponse == 201 || putResponse == 200) {
                log("Container created: " + containerURL);
            } else {
                log("Failed to create container. Response code: " + putResponse);
            }
        } catch(Exception e) {
            log("Exception while creating container: " + e.getMessage());
        }
    }

    /**
     * CArtAgO operation for publishing data within a .txt file in a Linked Data Platform container of the Solid pod
     * 
     * @param containerName The name of the container where the .txt file resource will be created
     * @param fileName The name of the .txt file resource to be created in the container
     * @param data An array of Object data that will be stored in the .txt file
     */
    @OPERATION
    public void publishData(String containerName, String fileName, Object[] data) {
        // Ensure container URL ends with a slash
        String resourceURL = podURL + containerName + "/" + fileName;
        String payload = createStringFromArray(data);
        try {
            URL url = new URL(resourceURL);
            HttpURLConnection putCon = (HttpURLConnection) url.openConnection();
            putCon.setRequestMethod("PUT");
            putCon.setDoOutput(true);
            putCon.setRequestProperty("Content-Type", "text/plain");
            OutputStream os = putCon.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.flush();
            os.close();
            int responseCode = putCon.getResponseCode();
            if(responseCode == 201 || responseCode == 200) {
                log("Published data to " + resourceURL);
            } else {
                log("Failed to publish data. Response code: " + responseCode);
            }
        } catch(Exception e) {
            log("Exception while publishing data: " + e.getMessage());
        }
    }

    /**
     * CArtAgO operation for reading data of a .txt file in a Linked Data Platform container of the Solid pod
     * 
     * @param containerName The name of the container where the .txt file resource is located
     * @param fileName The name of the .txt file resource that holds the data to be read
     * @param data An array whose elements are the data read from the .txt file
     */
    @OPERATION
    public void readData(String containerName, String fileName, OpFeedbackParam<Object[]> data) {
        data.set(readData(containerName, fileName));
    }

    /**
     * Method for reading data of a .txt file in a Linked Data Platform container of the Solid pod
     * 
     * @param containerName The name of the container where the .txt file resource is located
     * @param fileName The name of the .txt file resource that holds the data to be read
     * @return An array whose elements are the data read from the .txt file
     */
    public Object[] readData(String containerName, String fileName) {
        String resourceURL = podURL + containerName + "/" + fileName;
        try {
            URL url = new URL(resourceURL);
            HttpURLConnection getCon = (HttpURLConnection) url.openConnection();
            getCon.setRequestMethod("GET");
            getCon.setRequestProperty("Accept", "text/plain");
            int responseCode = getCon.getResponseCode();
            if(responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getCon.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                String content = sb.toString();
                log("Read data from " + resourceURL + ": " + content);
                return createArrayFromString(content);
            } else {
                log("Failed to read data. Response code: " + responseCode);
            }
        } catch(Exception e) {
            log("Exception while reading data: " + e.getMessage());
        }
        return new Object[0];
    }

    /**
     * Method that converts an array of Object instances to a string, 
     * e.g. the array ["one", 2, true] is converted to the string "one\n2\ntrue\n"
     *
     * @param array The array to be converted to a string
     * @return A string consisting of the string values of the array elements separated by "\n"
     */
    public static String createStringFromArray(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Method that converts a string to an array of Object instances computed by splitting the given string with delimiter "\n"
     * e.g. the string "one\n2\ntrue\n" is converted to the array ["one", "2", "true"]
     *
     * @param str The string to be converted to an array
     * @return An array consisting of string values that occur by splitting the string around "\n"
     */
    public static Object[] createArrayFromString(String str) {
        return str.split("\n");
    }


    /**
     * CArtAgO operation for updating data of a .txt file in a Linked Data Platform container of the Solid pod
     * The method reads the data currently stored in the .txt file and publishes in the file the old data along with new data 
     * 
     * @param containerName The name of the container where the .txt file resource is located
     * @param fileName The name of the .txt file resource that holds the data to be updated
     * @param data An array whose elements are the new data to be added in the .txt file
     */
    @OPERATION
    public void updateData(String containerName, String fileName, Object[] data) {
        Object[] oldData = readData(containerName, fileName);
        Object[] allData = new Object[oldData.length + data.length];
        System.arraycopy(oldData, 0, allData, 0, oldData.length);
        System.arraycopy(data, 0, allData, oldData.length, data.length);
        publishData(containerName, fileName, allData);
    }
}
