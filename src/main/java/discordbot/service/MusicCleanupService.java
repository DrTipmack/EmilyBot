/*
 * Copyright 2017 github.com/kaaz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package discordbot.service;

import discordbot.core.AbstractService;
import discordbot.db.WebDb;
import discordbot.db.controllers.CMusic;
import discordbot.db.controllers.CPlaylist;
import discordbot.db.model.OMusic;
import discordbot.main.BotContainer;
import discordbot.main.Launcher;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * cleans up unused cached music files
 */
public class MusicCleanupService extends AbstractService {

	public MusicCleanupService(BotContainer b) {
		super(b);
	}

	@Override
	public String getIdentifier() {
		return "music_cleanup_service";
	}

	@Override
	public long getDelayBetweenRuns() {
		return TimeUnit.DAYS.toMillis(1);
	}

	@Override
	public boolean shouldIRun() {
		return true;
	}

	@Override
	public void beforeRun() {
	}

	@Override
	public void run() {
		long olderThan = (System.currentTimeMillis() / 1000L) - TimeUnit.DAYS.toSeconds(14);
		try (ResultSet rs = WebDb.get().select("SELECT m.* " +
				" FROM music m " +
				" LEFT JOIN playlist_item pi ON pi.music_id = m.id " +
				" LEFT JOIN playlist pl ON pl.id = pi.playlist_id " +
				" LEFT JOIN guilds g ON g.id = pl.id AND g.active = 1 " +
				" WHERE g.id IS NULL " +
				" AND  m.lastplaydate < ? " +
				" AND m.file_exists = 1 " +
				" ORDER BY lastplaydate DESC", olderThan)) {
			while (rs.next()) {
				OMusic record = CMusic.fillRecord(rs);
				File file = new File(record.filename);
				if (file.exists()) {
					file.delete();
				}
				record.fileExists = 0;
				CPlaylist.deleteTrackFromPlaylists(record.id);
				CMusic.update(record);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Launcher.logToDiscord(e);
		}
	}

	@Override
	public void afterRun() {
	}
}