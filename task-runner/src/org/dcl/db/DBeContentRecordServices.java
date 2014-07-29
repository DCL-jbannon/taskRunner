package org.dcl.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;


public class DBeContentRecordServices implements IDBeContentRecordServices
{
	private static final String ACSaccesType = "acs";
	
	private static final String tableName = "econtent_record";
	private Connection conn;
	
	
	
	public DBeContentRecordServices(Connection conn) 
	{
		this.conn = conn;
	}
	
	public ArrayList<ArrayList<String>> getACSEcontentItemLessBySource(String source) throws SQLException
	{
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		
		String sql = " SELECT er.*"
				   + " FROM econtent_record as er LEFT JOIN econtent_item as ei on er.id = ei.recordId"
				   + " WHERE ei.recordId IS NULL AND accessType=? AND source=?";
		PreparedStatement stmt = this.conn.prepareStatement(sql);
		stmt.setString(1, DBeContentRecordServices.ACSaccesType);
		stmt.setString(2, source);
		stmt.execute();
		ResultSet resultSet = stmt.getResultSet();
		while(resultSet.next())
		{
			ArrayList<String> row = new ArrayList<String>();
			row.add(resultSet.getString("id"));
			row.add(resultSet.getString("title"));
			row.add(resultSet.getString("availableCopies"));
			row.add(resultSet.getString("isbn"));
			result.add(row);
		}

		return result;
	}
	
	
	public boolean insertEcontentItem(String filename, String acsId, String recordId, String item_type) throws SQLException
	{
		long unixTime = System.currentTimeMillis() / 1000L;
		String addedBy = "-1";
		
		PreparedStatement stmt =  this.conn.prepareStatement("INSERT INTO econtent_item (filename, acsId, recordId, item_type, addedBy, date_added, date_updated) VALUES (?, ?, ?, ?, ?, ?, ?)");
		
		stmt.setString(1, filename);
		stmt.setString(2, acsId);
		stmt.setString(3, recordId);
		stmt.setString(4, item_type);
		stmt.setString(5, addedBy);
		stmt.setString(6, "" + unixTime);
		stmt.setString(7, "" + unixTime);
		int rowsInserted = stmt.executeUpdate();
		if (rowsInserted == 1)
		{
			return true;
		}
		return false;
	}
	
	public boolean updateEcontentCover(String recordId, String coverFilename) throws SQLException
	{
		PreparedStatement stmt =  this.conn.prepareStatement("UPDATE econtent_record set cover=? WHERE id = ?");
		stmt.setString(1, coverFilename);
		stmt.setString(2, recordId);
		int rowsInserted = stmt.executeUpdate();
		if (rowsInserted == 1)
		{
			return true;
		}
		return false;
	}
	
	
	public boolean insertEContentRecord(String title, String source, String accessType, String isbn, String availableCopies, String Cover) throws SQLException
	{
		long unixTime = System.currentTimeMillis() / 1000L;

		String sql =  "INSERT INTO " + DBeContentRecordServices.tableName + "(`id`, `title`, `accessType`, `source`, `date_added`, `isbn`, `availableCopies`, `cover`)";
			   sql += "VALUES (NULL,?, ?, ?, ?, ?, ?, ?)";
	   try
	   {
		   PreparedStatement stmt = this.conn.prepareStatement(sql);
		   stmt.setString(1, "" + title);
		   stmt.setString(2, "" + accessType);
		   stmt.setString(3, "" + source);
		   stmt.setString(4, "" + unixTime);
		   stmt.setString(5, "" + isbn);
		   stmt.setString(6, "" + availableCopies);
		   stmt.setString(7, "" + Cover);
		   stmt.execute();
	   }
	   catch(Exception e)
	   {
		   System.out.println(e.getMessage() + "  " + title);
		   throw new SQLException(e.getMessage() + "  " + title);
	   }
	   return true;
	}
	
	public boolean insertEContentRecord(String title, String source, String accessType, String isbn, String availableCopies) throws SQLException
	{
		return this.insertEContentRecord(title, source, accessType, isbn, availableCopies, "");
	}

	public ArrayList<ArrayList<String>> getEcontentNoCoverBySource(String source) throws SQLException
	{
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
	
		String sql = " SELECT *"
				   + " FROM econtent_record"
				   + " WHERE accessType=? AND source=? AND cover=''";
		
		PreparedStatement stmt = this.conn.prepareStatement(sql);
		stmt.setString(1, DBeContentRecordServices.ACSaccesType);
		stmt.setString(2, source);
		stmt.execute();
		ResultSet resultSet = stmt.getResultSet();
		while(resultSet.next())
		{
			ArrayList<String> row = new ArrayList<String>();
			row.add(resultSet.getString("id"));
			row.add(resultSet.getString("title"));
			row.add(resultSet.getString("availableCopies"));
			row.add(resultSet.getString("isbn"));
			result.add(row);
		}
		return result;
	}
}