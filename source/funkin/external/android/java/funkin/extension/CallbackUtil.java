package funkin.extensions;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.haxe.extension.Extension;

import org.haxe.lime.HaxeObject;

public class CallbackUtil extends Extension
{
	/**
	 * Constant representing the event when the data folder is closed.
	 */
  public static int DATA_FOLDER_CLOSED = 0x01;

	private static HaxeObject haxeObject;
  private static final int FILE_PICKER_REQUEST_CODE = 0x504B;

  /**
	 * Initializes the callback object for handling Haxe callbacks.
	 *
	 * @param haxeObject  The HaxeObject instance to handle callbacks.
	 */
	public static void initCallBack(final HaxeObject haxeObject)
	{
		CallbackUtil.haxeObject = haxeObject;
	}

	public static void callMethod(String methodName)
	{
		if (haxeObject != null)
		{
			Object[] args = new Object[2];
			args[0] = methodName;
			args[1] = new Object[0];
			haxeObject.call("dispatchCallback", args);
		}
	}

	public static void callMethod(String methodName, Object arg1)
	{
		if (haxeObject != null)
		{
			Object[] _args = {arg1};
			Object[] args = new Object[2];
			args[0] = methodName;
			args[1] = _args;
			haxeObject.call("dispatchCallback", args);
		}
	}

	public static void callMethod(String methodName, Object arg1, Object arg2)
	{
		if (haxeObject != null)
		{
			Object[] _args = {arg1, arg2};
			Object[] args = new Object[2];
			args[0] = methodName;
			args[1] = _args;
			haxeObject.call("dispatchCallback", args);
		}
	}

	public static void callMethod(String methodName, Object arg1, Object arg2, Object arg3)
	{
		if (haxeObject != null)
		{
			Object[] _args = {arg1, arg2, arg3};
			Object[] args = new Object[2];
			args[0] = methodName;
			args[1] = _args;
			haxeObject.call("dispatchCallback", args);
		}
	}

	public static void callMethod(String methodName, Object arg1, Object arg2, Object arg3, Object arg4)
	{
		if (haxeObject != null)
		{
			Object[] _args = {arg1, arg2, arg3, arg4};
			Object[] args = new Object[2];
			args[0] = methodName;
			args[1] = _args;
			haxeObject.call("dispatchCallback", args);
		}
	}

	public static void callMethod(String methodName, Object[] args)
	{
		if (haxeObject != null)
		{
			Object[] _args = new Object[2];
			args[0] = methodName;
			args[1] = args;
			haxeObject.call("dispatchCallback", _args);
		}
	}

  /** Opens Android's native picker for importing any file type. */
  public static boolean openFilePicker()
  {
    if (mainActivity == null) return false;

    try
    {
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("*/*");
      mainActivity.startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_PICKER_REQUEST_CODE);
      return true;
    }
    catch (Exception e)
    {
      Log.e("FNFNFilePicker", "Unable to open Android file picker", e);
      return false;
    }
  }

  @Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data)
	{
    if (requestCode == FILE_PICKER_REQUEST_CODE)
    {
      if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null)
      {
        callMethod("onFilePickCancelled");
        return true;
      }

      final Uri selectedUri = data.getData();
      new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            File cachedFile = copySelectedFileToCache(selectedUri);
            callMethod("onFilePicked", cachedFile.getAbsolutePath(), getDisplayName(selectedUri));
          }
          catch (Exception e)
          {
            Log.e("FNFNFilePicker", "Failed to import selected file", e);
            callMethod("onFilePickCancelled");
          }
        }
      }, "FNFN-File-Import").start();
      return true;
    }

		callMethod("onActivityResult", requestCode, resultCode);
		return true;
	}

  private static File copySelectedFileToCache(Uri uri) throws IOException
  {
    if (uri == null || mainContext == null)
      throw new IOException("Selected file or Android context is unavailable");

    File cacheDirectory = new File(mainContext.getCacheDir(), "imports");
    if (!cacheDirectory.exists() && !cacheDirectory.mkdirs())
      throw new IOException("Unable to create import cache directory");

    String displayName = getDisplayName(uri);
    String safeName = new File(displayName).getName().replaceAll("[^a-zA-Z0-9._-]", "_");
    if (safeName.length() == 0) safeName = "imported_file";
    File output = new File(cacheDirectory, UUID.randomUUID().toString() + "_" + safeName);

    try (InputStream input = mainContext.getContentResolver().openInputStream(uri);
         FileOutputStream stream = new FileOutputStream(output))
    {
      if (input == null) throw new IOException("Android could not open the selected document");
      byte[] buffer = new byte[32 * 1024];
      int count;
      while ((count = input.read(buffer)) != -1) stream.write(buffer, 0, count);
      stream.flush();
    }
    catch (Exception e)
    {
      output.delete();
      if (e instanceof IOException) throw (IOException)e;
      throw new IOException("Unable to copy selected document", e);
    }

    return output;
  }

  private static String getDisplayName(Uri uri)
  {
    String name = null;
    Cursor cursor = null;
    try
    {
      cursor = mainContext.getContentResolver().query(uri, null, null, null, null);
      if (cursor != null && cursor.moveToFirst())
      {
        int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (column >= 0) name = cursor.getString(column);
      }
    }
    catch (Exception e)
    {
      Log.w("FNFNFilePicker", "Unable to read selected file name", e);
    }
    finally
    {
      if (cursor != null) cursor.close();
    }

    if (name == null || name.trim().length() == 0)
    {
      String segment = uri.getLastPathSegment();
      name = segment == null ? "imported_file" : segment;
    }
    return name;
  }
}
