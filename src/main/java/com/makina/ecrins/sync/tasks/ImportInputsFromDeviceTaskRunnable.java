package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * {@link AbstractTaskRunnable} implementation for fetching all inputs to be imported from a connected to device.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ImportInputsFromDeviceTaskRunnable extends AbstractTaskRunnable
{
	private static final Logger LOG = Logger.getLogger(ImportInputsFromDeviceTaskRunnable.class);
	
	private File inputsTempDir;
	
	@Override
	public void run()
	{
		setTaskStatus(new TaskStatus("MainWindow.status.pending", Status.STATUS_PENDING));
		
		try
		{
			fetchInputsFromDevice();
			
			if (uploadInputs())
			{
				setTaskStatus(new TaskStatus(100, "MainWindow.status.finish", Status.STATUS_FINISH));
			}
			else
			{
				setTaskStatus(new TaskStatus(100, "MainWindow.status.finish", Status.STATUS_FAILED));
			}
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			setTaskStatus(new TaskStatus(0, "MainWindow.status.failed", Status.STATUS_FAILED));
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setTaskStatus(new TaskStatus(0, "MainWindow.status.failed", Status.STATUS_FAILED));
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			setTaskStatus(new TaskStatus(0, "MainWindow.status.failed", Status.STATUS_FAILED));
		}
	}
	
	private void fetchInputsFromDevice() throws InterruptedException, IOException
	{
		this.inputsTempDir = new File(FileUtils.getTempDirectory(), "sync_inputs" + Long.toString(System.currentTimeMillis()));
		this.inputsTempDir.mkdir();
		FileUtils.forceDeleteOnExit(inputsTempDir);
		
		ADBCommand.getInstance().pull(com.makina.ecrins.sync.adb.FileUtils.getExternalStorageDirectory() + "Android/data/" + "com.makina.ecrins.poc" + "/inputs/", this.inputsTempDir.getAbsolutePath());
	}
	
	private boolean uploadInputs() throws JSONException, IOException
	{
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
		HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_TOKEN)));
		nameValuePairs.add(new BasicNameValuePair("data", "{}"));
		
		String urlImport = LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_SERVER_URL) + LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_IMPORT_URL);
		HttpPost httpPost = new HttpPost(urlImport);
		
		int currentInput = 0;
		
		Collection<File> inputFiles = FileUtils.listFiles(this.inputsTempDir, new IOFileFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				if (dir.getAbsolutePath().equals(inputsTempDir.getAbsolutePath()))
				{
					return name.startsWith("input_") && name.endsWith(".json");
				}
				else
				{
					return false;
				}
			}
			
			@Override
			public boolean accept(File file)
			{
				return file.getName().startsWith("input_") && file.getName().endsWith(".json");
			}
		},
		null);
		
		for (File inputFile : inputFiles)
		{
			setTaskStatus(new TaskStatus((int) (((double) currentInput / (double) inputFiles.size()) * 100), inputFile.getName(), Status.STATUS_PENDING));
			
			// reads input as JSON file
			nameValuePairs.set(1, new BasicNameValuePair("data", FileUtils.readFileToString(inputFile)));
			
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse httpResponse = httpClient.execute(httpPost);
			
			// checks if server response is valid
			StatusLine status = httpResponse.getStatusLine();
			
			if (status.getStatusCode() == HttpStatus.SC_OK)
			{
				// pulls content stream from response
				HttpEntity entity = httpResponse.getEntity();
				InputStream inputStream = entity.getContent();
				
				if (readInputStreamAsJson(inputFile.getName(), inputStream, entity.getContentLength(), currentInput, inputFiles.size()))
				{
					setTaskStatus(new TaskStatus("MainWindow.status.finish", Status.STATUS_PENDING));
				}
				else
				{
					setTaskStatus(new TaskStatus(inputFile.getName(), Status.STATUS_FAILED));
					return false;
				}
			}
			else
			{
				LOG.error("unable to upload input from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
				setTaskStatus(new TaskStatus(inputFile.getName(), Status.STATUS_FAILED));
				
				return false;
			}
			
			currentInput++;
		}
		
		return true;
	}
	
	private boolean readInputStreamAsJson(String inputName, InputStream in, long contentLength, int currentInput, int numberOfInputs)
	{
		OutputStream out = new OutputStream()
		{
			private StringBuilder string = new StringBuilder();
			
			@Override
			public void write(int b) throws IOException
			{
				this.string.append((char) b);
			}
			
			@Override
			public String toString()
			{
				return this.string.toString();
			}
		};
		
		byte[] buffer = new byte[1024];
		
		int len;
		long totalBytesRead = 0;
		
		try
		{
			while ((len = in.read(buffer)) >= 0)
			{
				out.write(buffer, 0, len);
				totalBytesRead += len;
				
				int currentProgress =  (int) (((double) totalBytesRead / (double) contentLength) * 100);
				
				setTaskStatus(new TaskStatus((int) (((double) currentInput / (double) numberOfInputs) * 100) + (currentProgress / numberOfInputs), inputName, Status.STATUS_PENDING));
			}
			
			out.flush();
			in.close();
			out.close();
			
			// Try to build the server response as JSON and check the status code
			JSONObject jsonResponse = new JSONObject(out.toString());
			int status = jsonResponse.getInt("status_code");
			
			return status == 0;
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			return false;
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			return false;
		}
	}
}
