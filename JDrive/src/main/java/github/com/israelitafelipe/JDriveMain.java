package github.com.israelitafelipe;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class JDriveMain {
	private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.
	private static final String GOOGLE_DRIVE = System.getProperty("user.home")+java.io.File.separator+"Google Drive"+java.io.File.separator;

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved credentials/ folder.
	 */
	private static final List<String> SCOPES = new ArrayList<>(DriveScopes.all());
	private static final String CLIENT_SECRET_DIR = "client_secret.json";

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If there is no client_secret.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = JDriveMain.class.getResourceAsStream(CLIENT_SECRET_DIR);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER)))
						.setAccessType("offline").build();
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	public static void main(String... args) throws IOException, GeneralSecurityException {
		java.io.File gd = new java.io.File(GOOGLE_DRIVE);
		if(!gd.exists()) {
			gd.mkdirs();
		}
		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Credential credential = getCredentials(HTTP_TRANSPORT);
		Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
				.build();

		FileList result = service.files().list()
				.setPageSize(1)
				.setQ("mimeType != 'application/vnd.google-apps.folder'")
				.setFields("nextPageToken, files(id, name)").execute();

		List<File> files = result.getFiles();
		if (files == null || files.isEmpty()) {
			System.out.println("No files found.");
		} else {
			System.out.println("Files:");
			for (File file : files) {
				try {
					System.out.printf("%s (%s)\n", file.getName(), file.getId());

					java.io.File f = new java.io.File(GOOGLE_DRIVE+file.getName());
					OutputStream outputStream = new FileOutputStream(f);
					service.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

}
