package edu.mit.media.funf.probe.builtin;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.probe.SynchronousProbe;

/**
 * Reads various information from the /proc file system. 
 * 
 * Based on the SystemSens Proc Sensor, written by Hossein Falaki.
 * @author Hossein Falaki
 * @author Alan Gardner
 */
public class ProcessStatisticsProbe extends SynchronousProbe {

    /** Address of the network devices stat */
    private static final String NETDEV_PATH  = "/proc/net/dev";
    
    /** Address of memory information file */
    private static final String MEMINFO_PATH = "/proc/meminfo";
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(Parameter.Builtin.PERIOD, 300L),
				new Parameter(Parameter.Builtin.START, 0L),
				new Parameter(Parameter.Builtin.END, 0L)
			};
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return null;
	}
	

	@Override
	protected Bundle getData() {
		Bundle data = new Bundle();
		data.putBundle("CPU_LOAD", getCpuLoad());
		data.putBundle("MEM_INFO", getMemInfo());
		data.putBundle("NET_DEV", getNetDev());
		return data;
	}    
	    

    public Bundle getMemInfo()
    {
    	
    	Bundle result = new Bundle();
    	
        StringTokenizer linest;
        String key, value;

        try
        {

			BufferedReader reader = new BufferedReader( new 
					InputStreamReader( new FileInputStream( MEMINFO_PATH ) ), 2048 );


            char[] buffer = new char[2024];
            reader.read(buffer, 0, 2000);
            
            
            StringTokenizer st = new StringTokenizer(
                    new String(buffer), "\n", false);

            for (int i = 0; i < st.countTokens(); i++)
            {

                linest = new StringTokenizer(st.nextToken());
                key = linest.nextToken();
                value = linest.nextToken();
                result.putLong(key, Long.valueOf(value));
            }

            reader.close();

        }
        catch (Exception e)
        {

            Log.e(TAG, "Exception parsing the file", e);
        }


        return result;    	
    }

    public ArrayList<Long> readProcessCpuTime(long processId)
    {

    	ArrayList<Long> res = null;
        long utime = 0;
        long stime = 0;
        
    	String line;
    	String[] toks;

		try
		{
			BufferedReader reader = new BufferedReader( new 
					InputStreamReader( new 
                        FileInputStream(
                            "/proc/" + processId + "/stat")), 512);

			while ( (line = reader.readLine()) != null )
			{
                toks = line.split(" ");

                utime = Long.parseLong(toks[13]);
                stime = Long.parseLong(toks[14]);
            }

			reader.close();

            res = new ArrayList<Long>();
            res.add(utime);
            res.add(stime);


		}
		catch( IOException ex )
		{
			Log.e(TAG, "Could not read /proc file", ex);
		}
        return res;

    }
    
    public Bundle getCpuLoad()
    {
    	Bundle result = new Bundle();
    	
    	float totalUsage, userUsage, niceUsage, systemUsage;
        Double cpuFreq = 0.0;
        long sTotal = 0;
    	
        String line;
    	String[] toks;
        String[] words;

        try
        {
			BufferedReader reader = new BufferedReader( new 
					InputStreamReader( new 
                        FileInputStream( "/proc/cpuinfo" ) ), 2048 );

			while ( (line = reader.readLine()) != null )
			{
				toks = line.split(" ");
                words = toks[0].split("\t");

                if (words[0].equals("BogoMIPS"))
                {
                    cpuFreq = Double.parseDouble(toks[1]);
                }
            }

            reader.close();

        }
        catch (IOException ioe)
        {
            Log.e(TAG, "Exception parsing /proc/cpuinfo", ioe);
        }
    	
		try
		{
			BufferedReader reader = new BufferedReader( new 
					InputStreamReader( new 
                        FileInputStream( "/proc/stat" ) ), 2048 );


		    long idle  = 0;
		    long user = 0;
		    long system = 0;
		    long nice = 0;
			
			while ( (line = reader.readLine()) != null )
			{
				toks = line.split(" ");

				if (toks[0].equals("cpu"))
				{
					long currUser, currNice, currSystem, currTotal,
                         currIdle;

					Bundle cpuObject = new Bundle();

					currUser = Long.parseLong(toks[2]);
					currNice = Long.parseLong(toks[3]);
					currSystem = Long.parseLong(toks[4]);
					currTotal = currUser + currNice + currSystem;
					currIdle = Long.parseLong(toks[5]);

					totalUsage = (currTotal - sTotal) * 100.0f / 
                        (currTotal - sTotal + currIdle - idle);
					userUsage = (currUser - user) * 100.0f / 
                        (currTotal - sTotal + currIdle - idle);
					niceUsage = (currNice - nice) * 100.0f / 
                        (currTotal - sTotal + currIdle - idle);
					systemUsage = (currSystem - system) * 100.0f / 
                        (currTotal - sTotal + currIdle - idle);


					sTotal = currTotal;
					idle = currIdle;
					user = currUser;
					nice = currNice;
					system = currSystem;

                    // Update the Status Object
                    //Status.setCPU(totalUsage);
					cpuObject.putFloat("total", totalUsage);
					cpuObject.putFloat("user", userUsage);
					cpuObject.putFloat("nice", niceUsage);
					cpuObject.putFloat("system", systemUsage);
                    cpuObject.putDouble("freq", cpuFreq);

					result.putBundle("cpu", cpuObject);
				} 
				else if (toks[0].equals("ctxt"))
				{
					String ctxt = toks[1];
					result.putLong("ContextSwitch", Long.valueOf(ctxt));
				}
				else if (toks[0].equals("btime"))
				{
					String btime = toks[1];
					result.putLong("BootTime", Long.valueOf(btime));
				}
				else if (toks[0].equals("processes"))
				{
					String procs = toks[1];
					result.putLong("Processes", Long.valueOf(procs));
				}				

			}
			result.putLong("CpuTotalTime", sTotal);
			reader.close();		

		}
		catch( IOException ex )
		{
			Log.e(TAG, "Could not read /proc file", ex);
		}

		return result;
    }


    /**
     * Parses and returns the contents of /proc/net/dev.
     * It first reads the content of the file in /proc/net/dev. 
     * This file contains a row for each network interface. 
     * Each row contains the number of bytes and packets that have
     * been sent and received over that network interface. This method
     * parses this file and returns a JSONObject that maps the network
     * interface name to this information.
     *
     * @return          JSONObject containing en entry for each
     *                      physical interface. 
     */
    public Bundle getNetDev()
    {

    	Bundle result = new Bundle();
        Bundle data;
        StringTokenizer linest;
        String devName, recvBytes, recvPackets, 
               sentBytes, sentPackets, zero;


        try
        {
                       
			BufferedReader reader = new BufferedReader( new 
					InputStreamReader( new FileInputStream( NETDEV_PATH ) ), 2048 );


            char[] buffer = new char[2024];
            reader.read(buffer, 0, 2000);
            
            
            StringTokenizer st = new StringTokenizer(
                    new String(buffer), "\n", false);

            //The first two lines of the file are headers
            zero = st.nextToken();
            zero = st.nextToken();

            for (int j = 0; j < 5; j++)
            {
                linest = new StringTokenizer(st.nextToken());
                devName = linest.nextToken();
                recvBytes = linest.nextToken();
                recvPackets = linest.nextToken();


                // Skip six tokens
                for (int i = 0; i < 6; i++) 
                    zero = linest.nextToken();

                sentBytes = linest.nextToken();
                sentPackets = linest.nextToken();



                data = new Bundle();
                
                data.putLong("RxBytes", Long.valueOf(recvBytes));
                data.putLong("RxPackets", Long.valueOf(recvPackets));

                data.putLong("TxBytes", Long.valueOf(sentBytes));
                data.putLong("TxPackets", Long.valueOf(sentPackets));
                
                result.putBundle(devName, data);

            }
            reader.close();

        }
        catch (Exception e)
        {

            Log.e(TAG, "Exception", e);
        }


        return result;
    }

	    
}
