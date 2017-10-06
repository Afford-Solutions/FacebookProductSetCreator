package com.google.bigquery.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.Bigquery.Jobs.Insert;
import com.google.api.services.bigquery.model.DatasetList;
import com.google.api.services.bigquery.model.GetQueryResultsResponse;
import com.google.api.services.bigquery.model.Job;
import com.google.api.services.bigquery.model.JobConfiguration;
import com.google.api.services.bigquery.model.JobConfigurationQuery;
import com.google.api.services.bigquery.model.JobReference;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse;
import com.google.api.services.bigquery.model.TableDataList;
import com.google.api.services.bigquery.model.TableList;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.DatasetList.Datasets;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest.Rows;
import com.google.api.services.bigquery.model.TableList.Tables;

public class BQOperations {

	public static final String PROJECT_ID = "stellar-display-145814";
	public static final String DATASET_ID = "dsp_output";
	
	public static Boolean StructureValidate(Bigquery bigquery, String TABLE_ID){
		try {
			TableList tables = bigquery.tables().list(PROJECT_ID, DATASET_ID).execute();	
			System.out.println(tables);
			if(null != tables && null != tables.getTables()){
				for(Tables table : tables.getTables()){
					if(table.getId().equalsIgnoreCase(PROJECT_ID + ":" + DATASET_ID + "." + TABLE_ID)){
						return true;
					}
				}
			}	
			return false;
		} catch (Exception e) {
			System.out.println(e);
			return false;	
		}
	}
	
	public static List<TableRow> GetUpdateRows(Bigquery bigquery, String TABLE_ID){
	
		try{
			
			String querysql = "SELECT catalog_id,product_set_name,filter_field,filter_value,operation,hostname,parse_client_id FROM ["+ PROJECT_ID +":"+ DATASET_ID +"."+ TABLE_ID +"]  where filter_field != \"product_type\";";
		
			JobReference jobId = startQuery(bigquery, PROJECT_ID, querysql);
		
			Job completedJob = checkQueryResults(bigquery, PROJECT_ID, jobId);
		
			return getQueryResults(bigquery, PROJECT_ID, completedJob);
	
		}catch(Exception e){
			System.out.println("Exception : TableResults.class - getResults Method");
			System.out.println(e);
			return null;
		}
			
	}
	
	public static JobReference startQuery(Bigquery bigquery, String projectId, String querySql) throws IOException {
		System.out.format("\nSelection Query Job: %s\n", querySql);
	
		Job job = new Job();
		JobConfiguration config = new JobConfiguration();
		JobConfigurationQuery queryConfig = new JobConfigurationQuery();
		config.setQuery(queryConfig);
	
		job.setConfiguration(config);
		queryConfig.setQuery(querySql);
	
		Insert insert = bigquery.jobs().insert(projectId, job);
		insert.setProjectId(projectId);
		JobReference jobId = insert.execute().getJobReference();
	
		System.out.format("\nJob ID of Query Job is: %s\n", jobId.getJobId());
	
		return jobId;
	}
	
	private static Job checkQueryResults(Bigquery bigquery, String projectId, JobReference jobId)
			throws IOException, InterruptedException {
		// Variables to keep track of total query time
		long startTime = System.currentTimeMillis();
		long elapsedTime;
	
		while (true) {
			Job pollJob = bigquery.jobs().get(projectId, jobId.getJobId()).execute();
			elapsedTime = System.currentTimeMillis() - startTime;
			System.out.format("Job status (%dms) %s: %s\n", elapsedTime, jobId.getJobId(),
					pollJob.getStatus().getState());
			if (pollJob.getStatus().getState().equals("DONE")) {
				return pollJob;
			}
			Thread.sleep(1000);
		}
	
	}
	
	private static List<TableRow> getQueryResults(Bigquery bigquery, String projectId, Job completedJob) {
	
		try {
			GetQueryResultsResponse queryResult = bigquery.jobs()
					.getQueryResults(projectId, completedJob.getJobReference().getJobId()).execute();
			int totRows = queryResult.getTotalRows().intValue();
			System.out.println("Total Rows fetched are : " + totRows);
			if (totRows > 0) {
				return queryResult.getRows();
			}
			else {
				return null;
			}
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	
	}
	
	public static Boolean insertDataRows(Bigquery bigquery, ArrayList<Rows> datachunk) {
	
		try {
			
			for(Rows row : datachunk){
				System.out.println(row.getJson().toString());
			}
			
			System.out.println(bigquery);
	
			TableDataInsertAllRequest content = new TableDataInsertAllRequest();
			content.setKind("bigquery#tableDataInsertAllRequest");
			content.setRows(datachunk);
			
			TableDataInsertAllResponse response = bigquery.tabledata().insertAll(PROJECT_ID, "docker_logs", "docker_facebook_logs", content).execute();
		
			System.out.println(response.toPrettyString());
			
			return true;
		
		} catch (Exception e) {
		
			System.out.println(e);
		
			return false;
			
		}
	
	}
	
}