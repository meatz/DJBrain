/**
 *   Copyright (C) 2006 Matthias Grawinkel <matthias@grawinkel.com>
 *	
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by  
 *   the Free Software Foundation; either version 2 of the License, or 
 *   (at your option) any later version.                                   
 *                                                                         
 */

package net.z0id.djbrain.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.z0id.djbrain.objects.Genre;
import net.z0id.djbrain.objects.Playlist;
import net.z0id.djbrain.objects.Suggestion;
import net.z0id.djbrain.objects.Track;
import net.z0id.djbrain.objects.TrackInPlaylist;
import net.z0id.djbrain.properties.DJProperties;

import org.apache.log4j.Logger;

import com.trolltech.qt.core.QObject;
import com.trolltech.qt.gui.QMessageBox;

/**
 * @author meatz
 * 
 */

public class DBConnection extends QObject {

	static final int DBTYPE_HSQLDB = 0;

//	static final int DBTYPE_MYSQL = 1;

	// private String db_path;

	private static Logger logger = Logger.getLogger(DBConnection.class);

	/**
	 * The internal version this db. Needed if the dblayout will change in a
	 * later release
	 */
	public static final int DBVERSION =2;

	private Connection conn;

	private static DBConnection dbc = null;

//	private static int DEBUGCOUNTER = 0;

	private DBConnection() {
	}

	/**
	 * @return Singleton Instance of DBConnection
	 */
	public static synchronized DBConnection getInstance() {
//		DEBUGCOUNTER++;
		return dbc;

	}

	/**
	 * 
	 */
	public static void init() {

		String databasetype = DJProperties.getProperty("databasetype");

		if (databasetype.equalsIgnoreCase("hsqldb")) {

			String db_path =DJProperties.getHSQLDBPath(); 

			String db_user = "sa";

			String db_pass = "";
			
			initHSQLDB(db_path, db_user, db_pass);

		} else if (databasetype.equalsIgnoreCase("mysql")) {

			String mysql_host = DJProperties.getProperty("mysql_host");
			int mysql_port = Integer.parseInt(DJProperties
					.getProperty("mysql_port"));
			String mysql_dbname = DJProperties.getProperty("mysql_dbname");
			String mysql_user = DJProperties.getProperty("mysql_user");
			String mysql_pass = DJProperties.getProperty("mysql_pass");

			initMYSQLDB(mysql_host, mysql_port, mysql_dbname, mysql_user,
					mysql_pass);

		} else {
			logger.error("no available database selected");
			System.exit(1);
		}

	}

	private static void initMYSQLDB(String db_host, int port, String db_name,
			String db_user, String db_pass) {
		
	}

	/**
	 * @param db_path
	 * @param db_user
	 * @param db_pass
	 */
	private static void initHSQLDB(String db_path, String db_user,
			String db_pass) {

		dbc = new DBConnection();
		boolean retry = true;

		while (retry) {

			try {
				Class.forName("org.hsqldb.jdbcDriver");
				dbc.conn = DriverManager.getConnection("jdbc:hsqldb:file:"
						+ db_path, db_user, // username
						db_pass); // password
				retry = false;
				break;

			} catch (ClassNotFoundException e) {
				// e.printStackTrace();
				logger.error("error while initializing database");
				QMessageBox.critical(null, dbc.tr("Error!"), dbc
						.tr("No JDBC Driver could be found"));

				logger.error("No JDBC Driver could be found");
				System.exit(1);
			} catch (SQLException e) {

				logger.error(e.getMessage());

				QMessageBox.StandardButton b  = QMessageBox.information(null, dbc.tr("Error!"), e
						.getMessage(),
						new QMessageBox.StandardButtons(QMessageBox.StandardButton.Retry,QMessageBox.StandardButton.Abort),
						QMessageBox.StandardButton.Retry);
				
				
				if (b == QMessageBox.StandardButton.Retry) {
					retry = true;

				} else {
					System.exit(1);
				}

			}
		}

		try {
			dbc
					.update("CREATE CACHED TABLE GENRES (\n"
							+ "ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 0) PRIMARY KEY,\n"
							+ "\n"
							+ "GENRE VARCHAR DEFAULT \'\' NOT NULL\n, UNIQUE(GENRE)"
							+ ");");

			// initialize blank genre
			dbc.update("INSERT INTO GENRES (GENRE) VALUES (\'\')");

		} catch (SQLException e) {

			if (!e.getMessage().startsWith("Table already exists")) {
				logger.error(e.getMessage());
			}
		}

		try {
			dbc
					.update("CREATE CACHED TABLE TRACKS (ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 0) PRIMARY KEY , ARTIST VARCHAR, TRACKNAME VARCHAR, LABEL VARCHAR, LENGTH VARCHAR, GENREID BIGINT DEFAULT \'0\', RELEASED INTEGER, BPM INTEGER, CATALOGNR VARCHAR, INVENTORYNR VARCHAR,COMMENT LONGVARCHAR,RATING INTEGER, MEDIATYPE VARCHAR,FILENAME VARCHAR, foreign key (GENREID) references GENRES(ID) ON DELETE SET DEFAULT );");
		} catch (SQLException e) {

			if (!e.getMessage().startsWith("Table already exists")) {
				logger.error(e.getMessage());
			}
		}

		try {
			dbc
					.update("CREATE CACHED TABLE PLAYLISTS ("
							+ "ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 0) PRIMARY KEY,"
							+ "PLAYLISTNAME VARCHAR," +
									" COMMENT VARCHAR, " +
									"UNIQUE(PLAYLISTNAME));");
		} catch (SQLException e) {

			if (!e.getMessage().startsWith("Table already exists")) {
				logger.error(e.getMessage());
			}
		}

		try {
			dbc
					.update("CREATE CACHED TABLE SUGGESTEDTRACKS ("
							+ "TRACKID BIGINT,"
							+ "SUGGESTEDTRACKID BIGINT,"
							+ "COMMENT VARCHAR,"
							+ "RATING INTEGER,"
							+ "foreign key (TRACKID) references TRACKS(ID) ON DELETE CASCADE,"
							+ "foreign key (SUGGESTEDTRACKID) references TRACKS(ID) ON DELETE CASCADE);");
		} catch (SQLException e) {

			if (!e.getMessage().startsWith("Table already exists")) {
				logger.error(e.getMessage());
			}
		}

		try {
			dbc
					.update("CREATE CACHED TABLE TRACKSINPLAYLISTS ("
							+ "PLAYLISTID BIGINT,"
							+ "TRACKID BIGINT,"
							+ "foreign key (PLAYLISTID) references PLAYLISTS(ID) ON DELETE CASCADE,"
							+ "foreign key (TRACKID) references TRACKS(ID) ON DELETE CASCADE);");
		} catch (SQLException e) {

			if (!e.getMessage().startsWith("Table already exists")) {
				logger.error(e.getMessage());

			}
		}

		try {
			dbc.update("CREATE CACHED TABLE METADATA ( DBVERSION INTEGER )");
			dbc.update("INSERT INTO METADATA (DBVERSION) VALUES ('  "
					+ DBVERSION + "') ");
		} catch (SQLException e) {

			if (!e.getMessage().startsWith("Table already exists")) {
				logger.error(e.getMessage());

			}
		}

		try {
			dbc.initPreparedStatements();
		} catch (SQLException e) {
			dbc.DBError(e);
		}
	}

	private PreparedStatement insertTrack;

	private PreparedStatement deleteTrack;

	private PreparedStatement insertPlaylist;

	private PreparedStatement deletePlaylist;

	private PreparedStatement insertSuggestedTrack;

	private PreparedStatement deleteSuggestedTrack;

	private PreparedStatement insertTrackinPlaylist;

	private PreparedStatement deleteTrackfromPlaylist;

	private PreparedStatement updateTrack;

	private PreparedStatement updatePlaylist;

	private PreparedStatement updateSuggestion;
	
	private PreparedStatement getSuggestedTrackIdsForTrack;
	
	private PreparedStatement getTrackForId;
	
	private PreparedStatement getPlaylistForId;
	private PreparedStatement getTrackIdsForPlaylist;
 private PreparedStatement getPlaylistNameForId;
	private PreparedStatement getTrackCount;
	private PreparedStatement getPlaylistIdForName;
	
	private PreparedStatement  getPlaylistItemCountForId;
	private PreparedStatement getAllPlaylists;
	private PreparedStatement getSuggestion;
	
	private void initPreparedStatements() throws SQLException {

		insertTrack = conn
				.prepareStatement("INSERT INTO TRACKS (ARTIST,TRACKNAME,LABEL,LENGTH,GENREID,RELEASED,BPM,CATALOGNR,INVENTORYNR,COMMENT,RATING,MEDIATYPE,FILENAME) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
		insertPlaylist = conn
				.prepareStatement("INSERT INTO PLAYLISTS (PLAYLISTNAME,COMMENT) VALUES (?,?)");
		insertTrackinPlaylist = conn
				.prepareStatement("INSERT INTO TRACKSINPLAYLISTS (PLAYLISTID,TRACKID) VALUES (?,?)");
		insertSuggestedTrack = conn
				.prepareStatement("INSERT INTO SUGGESTEDTRACKS (TRACKID,SUGGESTEDTRACKID, COMMENT, RATING) VALUES (?,?,?,?)");

		deleteTrack = conn.prepareStatement("DELETE FROM TRACKS WHERE ID=?");

		deletePlaylist = conn
				.prepareStatement("DELETE FROM PLAYLISTS WHERE ID=?");

		deleteSuggestedTrack = conn
				.prepareStatement("DELETE FROM SUGGESTEDTRACKS WHERE TRACKID=? AND SUGGESTEDTRACKID=?");

		deleteTrackfromPlaylist = conn
				.prepareStatement("DELETE FROM TRACKSINPLAYLISTS WHERE PLAYLISTID=? AND TRACKID=?");

		updateTrack = conn
				.prepareStatement("UPDATE TRACKS SET ARTIST=?,TRACKNAME=?,LABEL=?,LENGTH=?,GENREID=?,RELEASED=?,BPM=?,CATALOGNR=?,INVENTORYNR=?,COMMENT=?,RATING=?,MEDIATYPE=?,FILENAME=? WHERE ID=?;");

		updatePlaylist = conn
				.prepareStatement("UPDATE PLAYLISTS SET PLAYLISTNAME=?,COMMENT=? WHERE ID=?;");

		updateSuggestion = conn
				.prepareStatement("UPDATE SUGGESTEDTRACKS SET COMMENT=?, RATING=? WHERE TRACKID=? AND SUGGESTEDTRACKID=?");
		
		getSuggestedTrackIdsForTrack = conn.prepareStatement("SELECT suggestedtrackid FROM suggestedtracks WHERE trackid=?");
		getTrackForId = conn.prepareStatement("SELECT ID,ARTIST,TRACKNAME,LABEL,LENGTH,GENREID,RELEASED,BPM,CATALOGNR,INVENTORYNR,COMMENT,RATING,MEDIATYPE,FILENAME FROM TRACKS WHERE ID=?");
		getPlaylistForId = conn.prepareStatement("SELECT ID,PLAYLISTNAME,COMMENT FROM PLAYLISTS WHERE ID=?");
		getTrackIdsForPlaylist = conn.prepareStatement("SELECT TRACKID FROM TRACKSINPLAYLISTS WHERE PLAYLISTID=?");
		getPlaylistNameForId = conn.prepareStatement("SELECT PLAYLISTNAME FROM PLAYLISTS WHERE ID=?");
		getTrackCount = conn.prepareStatement("SELECT count (*) FROM TRACKS");
		getPlaylistIdForName = conn.prepareStatement("SELECT ID FROM PLAYLISTS WHERE PLAYLISTNAME=?");
		getPlaylistItemCountForId = conn.prepareStatement("select count (*) from Tracksinplaylists where playlistid =? group by playlistid");
		getAllPlaylists = conn.prepareStatement("SELECT ID,PLAYLISTNAME,COMMENT FROM PLAYLISTS");
		getSuggestion = conn.prepareStatement("SELECT COMMENT,RATING FROM SUGGESTEDTRACKS WHERE TRACKID=? AND SUGGESTEDTRACKID=?");
	}

	/**
	 * @param trackId
	 * @param suggestedTrackId
	 * @param comment
	 * @param rating
	 * @return true if the dbaction was successfull
	 * @throws SQLException
	 */
	public boolean insertSuggestedTrack(int trackId, int suggestedTrackId,
			String comment, int rating) {
		try {
			insertSuggestedTrack.setInt(1, trackId);

			insertSuggestedTrack.setInt(2, suggestedTrackId);

			if (comment == null) {
				comment = "";
			}
			insertSuggestedTrack.setString(3, comment);
			insertSuggestedTrack.setInt(4, rating);

			insertSuggestedTrack.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}
	};

	/**
	 * @param playlistId
	 * @param trackId
	 * @return true if the dbaction was successfull
	 */
	public boolean insertTrackinPlaylist(int playlistId, int trackId) {

		try {
			insertTrackinPlaylist.setInt(1, playlistId);

			insertTrackinPlaylist.setInt(2, trackId);

			insertTrackinPlaylist.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}
	}

	/**
	 * @param name
	 * @param comment
	 * @return true if the dbaction was successfull
	 */
	public boolean insertPlaylist(String name, String comment) {
		try {
			insertPlaylist.setString(1, name);
			insertPlaylist.setString(2, comment);
			insertPlaylist.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}

	};

	/**
	 * @param playlist
	 * @return true if the dbaction was successfull
	 */
	public boolean insertPlaylist(Playlist playlist) {

		if (insertPlaylist(playlist.getName(), playlist.getComment())) {

			int playlistId = getIdForPlaylist(playlist.getName());

			for (int trackId : playlist.getTracklist()) {
				insertTrackinPlaylist(playlistId, trackId);
			}
			return true;
		}

		return false;

		// try {
		// insertPlaylist.setString(1, name);
		// insertPlaylist.execute();
		// return true;
		// } catch (SQLException e) {
		// DBError(e);
		// return false;
		// }

	}

	/**
	 * @param name
	 *            of playlist
	 * @return id for this playlist, or -1 if an error occurs
	 */
	public int getIdForPlaylist(String name) {
		ResultSet rs = query("SELECT ID FROM PLAYLISTS WHERE PLAYLISTNAME='"
				+ name + "'");
		try {
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			DBError(e);
			return -1;
		}
	}

	/**
	 * @param id
	 * @param name
	 * @param comment
	 * @return return true if the dbaction was successfull
	 */
	public boolean updatePlaylist(int id, String name, String comment) {

		try {
			updatePlaylist.setString(1, name);
			updatePlaylist.setString(2, comment);
			updatePlaylist.setInt(3, id);
			updatePlaylist.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}

	}

	/**
	 * @param suggestion 
 	 * @return true if update was successfull
	 */
	public boolean updateSuggestion(Suggestion suggestion) {
		// UPDATE SUGGESTEDTRACKS SET COMMENT='foobar', RATING='3' WHERE
		// TRACKID='0' AND SUGGESTEDTRACKID='1'

		suggestion.getSuggestedTrackId();

		try {
			updateSuggestion.setString(1, suggestion.getComment());
			updateSuggestion.setInt(2, suggestion.getRating());
			updateSuggestion.setInt(3, suggestion.getTrackId());
			updateSuggestion.setInt(4, suggestion.getSuggestedTrackId());
			updateSuggestion.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}
	}

	/**
	 * @param track
	 * @return true if the dbaction was successfull
	 */
	public boolean insertTrack(Track track) {
		return insertTrack(track.getArtist(), track.getTrackname(), track
				.getLabel(), track.getLength(), track.getGenreId(), track
				.getReleased(), track.getBpm(), track.getCatalognr(), track
				.getInventorynr(), track.getComment(), track.getRating(), track
				.getMediatype(), track.getFilename());

	}

	/**
	 * @param artist
	 * @param trackname
	 * @param label
	 * @param length
	 * @param genreId
	 * @param genre
	 * @param released
	 * @param bpm
	 * @param catalognr
	 * @param inventorynr
	 * @param comment
	 * @param rating
	 * @param mediatype
	 * @param filename
	 * @return true if the dbaction was successfull
	 */
	public boolean insertTrack(String artist, String trackname, String label,
			String length, int genreId, int released, int bpm,
			String catalognr, String inventorynr, String comment, int rating,
			String mediatype, String filename) {
		try {
			insertTrack.setString(1, artist);

			insertTrack.setString(2, trackname);
			insertTrack.setString(3, label);
			insertTrack.setString(4, length);
			insertTrack.setInt(5, genreId);
			insertTrack.setInt(6, released);
			insertTrack.setInt(7, bpm);
			insertTrack.setString(8, catalognr);
			insertTrack.setString(9, inventorynr);
			insertTrack.setString(10, comment);
			insertTrack.setInt(11, rating);
			insertTrack.setString(12, mediatype);
			insertTrack.setString(13, filename);
			insertTrack.execute();

			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}

	};

	/**
	 * @param track
	 * @return true if the track was updated, false if the db has had an error
	 */
	public boolean updateTrack(Track track) {

		return updateTrackInDB(track.getId(), track.getArtist(), track
				.getTrackname(), track.getLabel(), track.getLength(), track
				.getGenreId(), track.getReleased(), track.getBpm(), track
				.getCatalognr(), track.getInventorynr(), track.getComment(),
				track.getRating(), track.getMediatype(), track.getFilename());

	}

	/**
	 * @param id
	 * @param artist
	 * @param trackname
	 * @param label
	 * @param length
	 * @param genre
	 * @param released
	 * @param bpm
	 * @param catalognr
	 * @param inventorynr
	 * @param comment
	 * @param rating
	 * @param mediatype
	 * @param filename
	 * @return true if the dbaction was successfull
	 */
	private boolean updateTrackInDB(int id, String artist, String trackname,
			String label, String length, int genreId, int released, int bpm,
			String catalognr, String inventorynr, String comment, int rating,
			String mediatype, String filename) {
		try {
			updateTrack.setString(1, artist);

			updateTrack.setString(2, trackname);
			updateTrack.setString(3, label);
			updateTrack.setString(4, length);
			updateTrack.setInt(5, genreId);
			updateTrack.setInt(6, released);
			updateTrack.setInt(7, bpm);
			updateTrack.setString(8, catalognr);
			updateTrack.setString(9, inventorynr);
			updateTrack.setString(10, comment);
			updateTrack.setInt(11, rating);
			updateTrack.setString(12, mediatype);
			updateTrack.setString(13, filename);
			updateTrack.setInt(14, id);
			updateTrack.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}
	};

	/**
	 * @param id
	 * @return true if the dbaction was successfull
	 */
	public boolean deleteTrack(int id) {
		try {
			deleteTrack.setInt(1, id);
			deleteTrack.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}
	}

	/**
	 * @param id
	 * @return true if the dbaction was successfull
	 */
	public boolean deletePlaylist(int id) {
		try {
			deletePlaylist.setInt(1, id);

			return deletePlaylist.execute();
		} catch (SQLException e) {
			DBError(e);
			return false;
		}
	}

	/**
	 * @param trackId
	 * @param suggestedTrack
	 * @param id
	 * @return true if the dbaction was successfull
	 */
	public boolean deleteSuggestedTrack(int trackId, int suggestedTrack) {

		try {
			deleteSuggestedTrack.setInt(1, trackId);
			deleteSuggestedTrack.setInt(2, suggestedTrack);
			deleteSuggestedTrack.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}

	}

	/**
	 * @param playlistid
	 * @param trackid
	 * @return true if the dbaction was successfull
	 */
	public boolean deleteTrackfromPlaylist(int playlistid, int trackid) {

		try {
			deleteTrackfromPlaylist.setInt(1, playlistid);
			deleteTrackfromPlaylist.setInt(2, trackid);
			deleteTrackfromPlaylist.execute();
			return true;
		} catch (SQLException e) {
			DBError(e);
			return false;
		}

	}
	
	
	/**
	 * @return DBVersion as int
	 */
	public int getDBVersion() {

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt
					.executeQuery("SELECT DBVERSION FROM METADATA");

			rs.next();
			int version = rs.getInt(1);
			 

			return version;
		} catch (SQLException e) {
			DBError(e);
			return 0;
		}
	
		
	}
		
		
	/**
	 * @param id
	 * @return List of suggested Tracks Integer IDs for this trackid, null if an
	 *         db error occurs
	 */
	public List<Integer> getSuggestedTrackIdsForTrackId(int id) {
		try {
			 			
			getSuggestedTrackIdsForTrack.setInt(1, id);
			
			ResultSet rs = getSuggestedTrackIdsForTrack.executeQuery();

			List<Integer> foo = new ArrayList<Integer>();

			while (rs.next()) {
				foo.add(rs.getInt(1));
			}

			return foo;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @param id
	 * @return List of suggested Tracks for this trackid, null if an db error
	 *         occurs
	 */
	public List<Track> getSuggestedTracksForTrackId(int id) {

		List<Integer> foobar = getSuggestedTrackIdsForTrackId(id);

		List<Track> tracks = new ArrayList<Track>();
		for (Integer integer : foobar) {
			Track track = getTrackForId(integer);
			if (track != null) {
				tracks.add(track);
			}
		}

		return tracks;
	}

	/**
	 * @param id
	 * @return List of suggested Tracks for this trackid, null if an db error
	 *         occurs
	 */
	public List<Integer> getAllTrackIds() {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT ID FROM TRACKS");

			List<Integer> foo = new ArrayList<Integer>();

			while (rs.next()) {
				foo.add(rs.getInt(1));
			}

			return foo;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @return the highest trackid in db, this id should also be the last added
	 *         trackid! returns -1 if an dberror occured
	 */
	public int getHighestTrackId() {

		Statement stmt;
		try {
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("select max (id) from tracks");
			rs.next();

			return rs.getInt(1);
		} catch (SQLException e) {

			DBError(e);
			return -1;
		}
	}

	/**
	 * @param id
	 * @return Track from db
	 */
	public Track getTrackForId(int id) {
	 
		try {
			 	getTrackForId.setInt(1, id);
			ResultSet rs = getTrackForId.executeQuery();
			rs.next();
			Track track = new Track();

			track.setId(rs.getInt("ID"));
			track.setArtist(rs.getString("ARTIST"));
			track.setTrackname(rs.getString("TRACKNAME"));
			track.setLabel(rs.getString("LABEL"));
			track.setLength(rs.getString("LENGTH"));
			track.setGenreId(rs.getInt("GENREID"));
			track.setReleased(rs.getInt("RELEASED"));
			track.setBpm(rs.getInt("BPM"));
			track.setCatalognr(rs.getString("CATALOGNR"));
			track.setInventorynr(rs.getString("INVENTORYNR"));
			track.setComment(rs.getString("COMMENT"));
			track.setRating(rs.getInt("RATING"));
			track.setMediatype(rs.getString("MEDIATYPE"));
			track.setFilename(rs.getString("FILENAME"));

			return track;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @param filter
	 * @return List with tracks filtered by 'filter'
	 */
	public List<Track> getAllTracks(String filter) {
		try {
		 
			ResultSet rs;
			
			if (filter != null) {
				rs = conn.prepareStatement("SELECT ID,ARTIST,TRACKNAME,LABEL,LENGTH,GENREID,RELEASED,BPM,CATALOGNR,INVENTORYNR,COMMENT,RATING,MEDIATYPE,FILENAME FROM TRACKS  WHERE " + filter).executeQuery();	 
			}else{
				rs = conn.prepareStatement("SELECT ID,ARTIST,TRACKNAME,LABEL,LENGTH,GENREID,RELEASED,BPM,CATALOGNR,INVENTORYNR,COMMENT,RATING,MEDIATYPE,FILENAME FROM TRACKS").executeQuery();	 
			}
			  

			List<Track> tracklist = new ArrayList<Track>();

			while (rs.next()) {

				Track track = new Track();

				track.setId(rs.getInt("ID"));
				track.setArtist(rs.getString("ARTIST"));
				track.setTrackname(rs.getString("TRACKNAME"));
				track.setLabel(rs.getString("LABEL"));
				track.setLength(rs.getString("LENGTH"));
				track.setGenreId(rs.getInt("GENREID"));
				track.setReleased(rs.getInt("RELEASED"));
				track.setBpm(rs.getInt("BPM"));
				track.setCatalognr(rs.getString("CATALOGNR"));
				track.setInventorynr(rs.getString("INVENTORYNR"));
				track.setComment(rs.getString("COMMENT"));
				track.setRating(rs.getInt("RATING"));
				track.setMediatype(rs.getString("MEDIATYPE"));
				track.setFilename(rs.getString("FILENAME"));
				tracklist.add(track);
			}
			return tracklist;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}

	}

	/**
	 * @return List of ALL Tracks, null if an db error occurs
	 */
	public List<Track> getAllTracks() {
		return getAllTracks(null);
	}

	/**
	 * @param playlistid
	 * @param filter
	 * @return  List<Track>
	 */
	public List<Track> getAllTracksForPlaylistId(int playlistid,
			String filter) {
	 
		try {
			ResultSet rs;
			if (filter != null) {
				PreparedStatement ps = conn.prepareStatement("SELECT distinct ID,ARTIST,TRACKNAME,LABEL,LENGTH,GENREID,RELEASED,BPM,CATALOGNR,INVENTORYNR,COMMENT,RATING,MEDIATYPE,FILENAME FROM TRACKS,TRACKSINPLAYLISTS WHERE TRACKS.ID = TRACKSINPLAYLISTS.TRACKID AND PLAYLISTID = ? AND " + filter);
				ps.setInt(1, playlistid);
				rs = ps.executeQuery();
			}else{
				PreparedStatement ps = conn.prepareStatement("SELECT distinct ID,ARTIST,TRACKNAME,LABEL,LENGTH,GENREID,RELEASED,BPM,CATALOGNR,INVENTORYNR,COMMENT,RATING,MEDIATYPE,FILENAME FROM TRACKS,TRACKSINPLAYLISTS WHERE TRACKS.ID = TRACKSINPLAYLISTS.TRACKID AND PLAYLISTID = ?");
				ps.setInt(1, playlistid);
				rs = ps.executeQuery();
			}

			
			List<Track> tracklist = new ArrayList<Track>();

			while (rs.next()) {

				Track track = new Track();

				track.setId(rs.getInt("ID"));
				track.setArtist(rs.getString("ARTIST"));
				track.setTrackname(rs.getString("TRACKNAME"));
				track.setLabel(rs.getString("LABEL"));
				track.setLength(rs.getString("LENGTH"));
				track.setGenreId(rs.getInt("GENREID"));
				track.setReleased(rs.getInt("RELEASED"));
				track.setBpm(rs.getInt("BPM"));
				track.setCatalognr(rs.getString("CATALOGNR"));
				track.setInventorynr(rs.getString("INVENTORYNR"));
				track.setComment(rs.getString("COMMENT"));
				track.setRating(rs.getInt("RATING"));
				track.setMediatype(rs.getString("MEDIATYPE"));
				track.setFilename(rs.getString("FILENAME"));
				tracklist.add(track);
			}
			return tracklist;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @param playlistid
	 * @return List of all Tracks which are in the specified playlist, null if
	 *         an db error occurs
	 */
	public List<Track> getAllTracksForPlaylistId(int playlistid) {

		return getAllTracksForPlaylistId(playlistid, null);
	}

	/**
	 * @param id
	 *            of the playlist
	 * @return Playlist Object which contains the Playlist for the specified id,
	 *         null if a dberror occurs
	 */
	public Playlist getPlaylistForId(int id) {
		 
		try {
			getPlaylistForId.setInt(1, id);
			ResultSet rs = getPlaylistForId.executeQuery();
			
			rs.next();

			Playlist playlist = new Playlist();

			// int id = rs.getInt("ID");
			playlist.setId(id);
			playlist.setName(rs.getString("PLAYLISTNAME"));
			playlist.setComment(rs.getString("COMMENT"));
			playlist.setItemCount(getPlaylistItemCountForId(id));
			getTrackIdsForPlaylist.setInt(1, id);
			rs = getTrackIdsForPlaylist.executeQuery();
			
			while (rs.next()) {
				playlist.addTrack(rs.getInt(1));
			}

			return playlist;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @param id
	 * @return Name of this playlist
	 */
	public String getPlaylistNameForId(int id) {
		
		try {
			getPlaylistNameForId.setInt(1, id);

			ResultSet rs = getPlaylistNameForId.executeQuery();
			
			rs.next();

			return rs.getString(1);
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @param playlistname
	 * @return the id for the given playlistname, -2 if it does not exist
	 */
	public int getPlaylistIdForName(String playlistname) {
		 
		try {
			 
			getPlaylistIdForName.setString(1, playlistname);

			ResultSet rs = getPlaylistIdForName.executeQuery();
			rs.next();
			return rs.getInt(1);

		} catch (SQLException e) {
			DBError(e);
			return -2;
		}
	}

	/**
	 * @return number of Tracks in the DB, -1 if a db error occurs
	 */
	public int getTrackCount() {

		try {

			ResultSet rs = getTrackCount.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0; // no tracks in playlist...
			}
		} catch (SQLException e) {
			DBError(e);
			return -1;
		}
	}

	/**
	 * @param id
	 *            the id of the playlist
	 * @return gives the total number of Tracks in this playlist, -1 if a db
	 *         error occurs
	 */
	public int getPlaylistItemCountForId(int id) {
		try {
			getPlaylistItemCountForId.setInt(1, id);
			ResultSet rs = getPlaylistItemCountForId.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0; // no tracks in playlist...
			}

		} catch (SQLException e) {
			DBError(e);
			return -1;
		}

	}

	/**
	 * @return ArrayList with all Playlists PlaylistItems, null if a db error
	 *         occurs
	 */
	public List<Playlist> getAllPlaylists() {
		try {

			ResultSet rs = getAllPlaylists.executeQuery();

			List<Playlist> playlists = new ArrayList<Playlist>();

			while (rs.next()) {

				Playlist pl = new Playlist();
				int id = rs.getInt(1);
				pl.setId(id);
				pl.setName(rs.getString(2));
				pl.setComment(rs.getString(3));
				pl.setItemCount(getPlaylistItemCountForId(id));

				getTrackIdsForPlaylist.setInt(1, id);
				ResultSet rs2 = getTrackIdsForPlaylist.executeQuery();

				List<Integer> tracklist = new ArrayList<Integer>();
				while (rs2.next()) {
					tracklist.add(rs2.getInt(1));
				}

				pl.setTracklist(tracklist);

				playlists.add(pl);
			}
			return playlists;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @return ArrayList with all Playlists PlaylistItems, null if a db error
	 *         occurs
	 */
	public List<Suggestion> getAllSuggestions() {
		Statement stmt;
		try {
			stmt = conn.createStatement();

			ResultSet rs = stmt
					.executeQuery("SELECT TRACKID,SUGGESTEDTRACKID,COMMENT,RATING FROM SUGGESTEDTRACKS;");

			List<Suggestion> suggestions = new ArrayList<Suggestion>();

			while (rs.next()) {

				Suggestion s = new Suggestion(rs.getInt(1), rs.getInt(2), rs
						.getString(3), rs.getInt(4));

				suggestions.add(s);
			}
			return suggestions;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}
	
	/**
	 * @param currentTrackId
	 * @param suggestedTrackId
	 * @return suggestion
	 */
	public Suggestion getSuggestion(int currentTrackId, int suggestedTrackId) {
		Suggestion suggestion = new Suggestion();
		Statement stmt;
		try {
			stmt = conn.createStatement();

			getSuggestion.setInt(1, currentTrackId);
			getSuggestion.setInt(2, suggestedTrackId);
			ResultSet rs = getSuggestion.executeQuery();
			rs.next();
		
			suggestion.setTrackId(currentTrackId);
			suggestion.setSuggestedTrackId(suggestedTrackId);
			suggestion.setComment(rs.getString(1));
			suggestion.setRating(rs.getInt(2));
			
			return suggestion;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @return ArrayList with all Genres , null if a db error occurs
	 */
	public List<Genre> getAllGenres() {
		Statement stmt;
		try {
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT ID,GENRE FROM GENRES ");

			List<Genre> genres = new ArrayList<Genre>();

			while (rs.next()) {

				Genre g = new Genre(rs.getInt(1), rs.getString(2));
				genres.add(g);
			}

			return genres;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @return ArrayList with all Genres , null if a db error occurs
	 */
	public List<TrackInPlaylist> getAllTracksInPlaylists() {
		Statement stmt;
		try {
			stmt = conn.createStatement();

			ResultSet rs = stmt
					.executeQuery("SELECT PLAYLISTID,TRACKID FROM TRACKSINPLAYLISTS;");

			List<TrackInPlaylist> l = new ArrayList<TrackInPlaylist>();

			while (rs.next()) {
				TrackInPlaylist t = new TrackInPlaylist(rs.getInt(1), rs
						.getInt(2));
				l.add(t);
			}
			return l;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	// /**
	// * dump the db to stdout
	// */
	// public void dumpBD() {
	//
	// System.out.println(getDump());
	// }

	/**
	 * @return a dump of the Database
	 */
	public String getDump() {

		StringBuffer buf = new StringBuffer();
		
//		StringBuffer buf = new StringBuffer(" getInstance was called: "
//				+ DEBUGCOUNTER + " times since program has started \n");
		
		Statement stmt;
		try {

			buf
					.append("------------------------------------------------------------------------  METADATA:  ------------------------------------------------------------------------------------------------------------------\n");

			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * from METADATA");
			rs.next();
			buf.append("DBVERSION: " + rs.getString(1) + "\n");

			// GET TRACKS
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT * from tracks");
			ResultSetMetaData rsm = rs.getMetaData();
			int columns = rsm.getColumnCount();

			buf
					.append("------------------------------------------------------------------------  TRACKS  ------------------------------------------------------------------------------------------------------------------\n");

			for (int i = 1; i <= columns; i++) {
				if (i != 1) {
					buf.append("\t");
				}
				buf.append(rsm.getColumnName(i));
			}

			buf
					.append("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
			while (rs.next()) {
				buf.append("\n");
				for (int i = 1; i <= columns; i++) {
					if (i != 1) {
						buf.append("\t");
					}
					buf.append(rs.getString(i));
				}
			}

			// GETPLAYLISTS
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT * from PLAYLISTS");
			rsm = rs.getMetaData();
			columns = rsm.getColumnCount();

			buf
					.append("\n \n------------------------------------------------------------------------  PLAYLISTS  ------------------------------------------------------------------------------------------------------------------ \n");

			for (int i = 1; i <= columns; i++) {
				if (i != 1) {
					buf.append("\t");
				}
				buf.append(rsm.getColumnName(i));
			}

			buf
					.append("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

			while (rs.next()) {
				buf.append("\n");
				for (int i = 1; i <= columns; i++) {
					if (i != 1) {
						buf.append("\t");
					}
					buf.append(rs.getString(i));
				}
			}

			// GET TRACKSINPLAYLISTS
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT * from TRACKSINPLAYLISTS");
			rsm = rs.getMetaData();
			columns = rsm.getColumnCount();

			buf
					.append("\n \n------------------------------------------------------------------------  TRACKSINPLAYLISTS  ------------------------------------------------------------------------------------------------------------------ \n");

			for (int i = 1; i <= columns; i++) {
				if (i != 1) {
					buf.append("\t");
				}
				buf.append(rsm.getColumnName(i));
			}

			buf
					.append("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
			while (rs.next()) {
				buf.append("\n");
				for (int i = 1; i <= columns; i++) {
					if (i != 1) {
						buf.append("\t");
					}
					buf.append(rs.getString(i));
				}
			}

			// GET SUGGESTEDTRACKS
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT * from SUGGESTEDTRACKS");
			rsm = rs.getMetaData();
			columns = rsm.getColumnCount();

			buf
					.append("\n \n------------------------------------------------------------------------  SUGGESTEDTRACKS  ------------------------------------------------------------------------------------------------------------------ \n");
			for (int i = 1; i <= columns; i++) {
				if (i != 1) {
					buf.append("\t");
				}
				buf.append(rsm.getColumnName(i));
			}

			buf
					.append("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
			while (rs.next()) {
				buf.append("\n");
				for (int i = 1; i <= columns; i++) {
					if (i != 1) {
						buf.append("\t");
					}
					buf.append(rs.getString(i));
				}
			}

			// GET GENRES
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT * from GENRES");
			rsm = rs.getMetaData();
			columns = rsm.getColumnCount();

			buf
					.append("\n \n------------------------------------------------------------------------  GENRES  ------------------------------------------------------------------------------------------------------------------ \n");

			for (int i = 1; i <= columns; i++) {
				if (i != 1) {
					buf.append("\t");
				}
				buf.append(rsm.getColumnName(i));
			}

			buf
					.append("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
			while (rs.next()) {
				buf.append("\n");
				for (int i = 1; i <= columns; i++) {
					if (i != 1) {
						buf.append("\t");
					}
					buf.append(rs.getString(i));
				}
			}

		} catch (SQLException e) {
			buf.append(" Error while printing DB-contents: " + e.getMessage());
			e.printStackTrace();
		}

		return buf.toString();
	}

	private String lastError = "";

	/**
	 * @return last Errormessage should be syncronized due to synconization of
	 *         getInstance()
	 */
	public String getLastError() {
		return lastError;
	}

	/**
	 * @param e
	 */
	public void DBError(SQLException e) {
		logger.error(e.getMessage());
		lastError = e.getMessage();
	}

	/**
	 * @param query
	 * @param mediatype
	 * @param rating
	 * @param genre
	 * @return list with specified tracks
	 */
	public List<Track> getTracksForSearchQuery(String query) {
		try {
		
			String queryString = "SELECT ID,ARTIST,TRACKNAME,LABEL,LENGTH,GENREID,RELEASED,BPM,CATALOGNR,INVENTORYNR,"
					+ "COMMENT,RATING,MEDIATYPE,FILENAME FROM TRACKS "
					+ "WHERE ( ARTIST like '%%"
					+ query
					+ "%%' or "
					+ "TRACKNAME like '%%"
					+ query
					+ "%%'or "
					+ "LABEL like '%%"
					+ query
					+ "%%' or "
					+ "CATALOGNR like '%%"
					+ query
					+ "%%' or"
					+ " INVENTORYNR like '%%"
					+ query
					+ "%%' or"
					+ " COMMENT like '%%" + query + "%%')";

			ResultSet rs = conn.prepareStatement(queryString).executeQuery();

			List<Track> tracklist = new ArrayList<Track>();

			while (rs.next()) {

				Track track = new Track();

				track.setId(rs.getInt("ID"));
				track.setArtist(rs.getString("ARTIST"));
				track.setTrackname(rs.getString("TRACKNAME"));
				track.setLabel(rs.getString("LABEL"));
				track.setLength(rs.getString("LENGTH"));
				track.setGenreId(rs.getInt("GENREID"));
				track.setReleased(rs.getInt("RELEASED"));
				track.setBpm(rs.getInt("BPM"));
				track.setCatalognr(rs.getString("CATALOGNR"));
				track.setInventorynr(rs.getString("INVENTORYNR"));
				track.setComment(rs.getString("COMMENT"));
				track.setRating(rs.getInt("RATING"));
				track.setMediatype(rs.getString("MEDIATYPE"));
				track.setFilename(rs.getString("FILENAME"));
				tracklist.add(track);
			}
			return tracklist;
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @param queryString
	 * @return ResultSet for this query
	 * @throws SQLException
	 */
	public synchronized ResultSet query(String queryString) {

		try {
			
			return conn.prepareStatement(queryString).executeQuery();
		} catch (SQLException e) {
			DBError(e);
			return null;
		}
	}

	/**
	 * @param expression
	 * @throws SQLException
	 */
	public synchronized void update(String expression) throws SQLException {

		Statement stmt = conn.createStatement();

		stmt.executeUpdate(expression);

		stmt.close();
	}

	/**
	 * Shut down the database, call this to make sure that db is written
	 * correctly
	 */
	public void shutdown() {

		Statement st;
		try {
			// db writes out to files and performs clean shuts down
			// otherwise there will be an unclean shutdown
			// when program ends

			st = conn.createStatement();
			st.execute("SHUTDOWN");
			conn.close(); // if there are no other open connection
		} catch (SQLException e) {
			// hmmm what should be done here...its only called when djbrain is
			// closed, so ignoring would be ok here ;)
			e.printStackTrace();
		}

	}

	

}
