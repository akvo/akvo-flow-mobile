package org.akvo.flow.util;

import android.util.Log;

/**
 * Created by EnderCrypt on 19/05/16.
 */
public class AutoLog
{
	private static String logo =
			"                _        _                 \n" +
					"     /\\        | |      | |                \n" +
					"    /  \\  _   _| |_ ___ | |     ___   __ _ \n" +
					"   / /\\ \\| | | | __/ _ \\| |    / _ \\ / _` |\n" +
					"  / ____ \\ |_| | || (_) | |___| (_) | (_| |\n" +
					" /_/    \\_\\__,_|\\__\\___/|______\\___/ \\__, |\n" +
					"                                      __/ |\n" +
					"                                     |___/ ";
	private static long timeIndex = System.currentTimeMillis();


	public static void introduce()
	{
		StringBuilder sb = new StringBuilder();
		sb.append('\n');
		for (int i=0;i<16;i++)
		{
			sb.append("-\n");
		}
		sb.append(logo);
		Log.i(AutoLog.class.getSimpleName(),sb.toString());
	}

	public static void info(String message)
	{
		print(Log.INFO, message);
	}

	public static void info(String label, String message)
	{
		info(label+": "+message);
	}

	public static void debug(String message)
	{
		print(Log.DEBUG, message);
	}

	public static void debug(String label, String message)
	{
		debug(label+": "+message);
	}

	public static void error(String message)
	{
		print(Log.ERROR, message);
	}

	public static void error(String label, String message)
	{
		error(label+": "+message);
	}

	public static void warn(String message)
	{
		print(Log.WARN, message);
	}

	public static void warn(String label, String message)
	{
		warn(label+": "+message);
	}

	private static void print(int level, String message)
	{
		StackTraceElement ste = getStackTrace();

		String fullName = ste.getClassName();
		String packageName = fullName.substring(0,fullName.lastIndexOf('.'));
		String className = fullName.substring(fullName.lastIndexOf('.')+1);
		String methodName = ste.getMethodName();

		message = message.replace("(QUALIFIED)",fullName);
		message = message.replace("(PACKAGE)",packageName);
		message = message.replace("(CLASS)",className);
		message = message.replace("(METHOD)",methodName);
		message = message.replace("(LINE)", String.valueOf(ste.getLineNumber()));

		String info = "("+getTimeElapsed()+" sec) " + fullName+" (Method: "+methodName+") Line: "+ste.getLineNumber();

		Log.println(level, AutoLog.class.getSimpleName(), info+'\n'+message);
	}

	private static StackTraceElement getStackTrace()
	{
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stack[5];
		return caller;
	}

	private static double getTimeElapsed()
	{
		long newTime = System.currentTimeMillis();
		int diff = (int)(newTime - timeIndex);
		timeIndex = newTime;
		double diff_second = diff/1000.0;
		return Math.round(diff_second*100.0)/100.0;
	}
}
