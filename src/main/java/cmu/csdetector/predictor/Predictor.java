package cmu.csdetector.predictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class Predictor {

	private String model;
	private int maxTokens;
	private double temperature;
	private String apiKey;

	public Predictor(String model, double temperature, String apiKey, int maxTokens) {
		this.model = model;
		this.maxTokens = maxTokens;
		this.temperature = temperature;
		this.apiKey = apiKey;
	}


	/**

		Sends a completion request to the OpenAI API using the specified prompt and returns the text response of the model.
		@param prompt the prompt to send to the model
		@return the text response of the model
		@throws IOException if there is an error sending the completion request or reading the response
	*/
    public String getCompletion(String prompt) throws IOException {

        String url = "https://api.openai.com/v1/" + model + "/completions";
        String stop = "\n";

        String payload = "{"
                + "\"model\": \"" + this.model + "\","
                + "\"prompt\": \"" + prompt + "\","
                + "\"max_tokens\": " + this.maxTokens + ","
                + "\"temperature\": " + this.temperature + ","
                + "\"stop\": \"" + stop + "\""
                + "}";

        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(payload.getBytes());
        os.flush();
        os.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String messageContent = parseResponseJSON(response.toString());

        return messageContent;
    }

    public String buildPrompt(String methodBody) {
    	return "You are an AI model designed to understand"
    			+ " java code. You will be given a snippet of "
    			+ "a method body in Java and your ONLY job is to analyze "
    			+ "that method body and suggest one suitable name for that method."
    			+ " You will return only the suggested name for the method based "
    			+ "on the method body. Your response must not be justifications"
    			+ " of your answer. only the method name. You must put the suggested "
    			+ "method body name in quotes and return it.\n\n"
    			+ "Here is the method body:\n\n ```" + methodBody
    			+ "\n\n```"
    			+ "Now return only the suggested name for the method "
    			+ "based on the method body";
    }

    public String predictMethodName(String methodBody) throws IOException {
    	String prompt = this.buildPrompt(methodBody);
    	try {
			return this.getCompletion(prompt);
		} catch (IOException e) {
			throw e;
		}
    }

    private static String parseResponseJSON(String responseJSON) {
        JSONObject jsonResponse = new JSONObject(responseJSON);
        JSONArray choicesArray = jsonResponse.getJSONArray("choices");
        JSONObject choiceObject = choicesArray.getJSONObject(0);

        return choiceObject.getString("text").trim();
    }
}