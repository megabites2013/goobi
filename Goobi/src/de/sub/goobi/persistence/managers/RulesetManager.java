package de.sub.goobi.persistence.managers;
/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - http://www.intranda.com
 *          - http://digiverso.com 
 *          - http://www.goobi.org
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.log4j.Logger;
import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Ruleset;

import de.sub.goobi.helper.exceptions.DAOException;

public class RulesetManager implements IManager, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -7947612330017761755L;

    private static final Logger logger = Logger.getLogger(RulesetManager.class);

    public static Ruleset getRulesetById(int id) throws DAOException {
        Ruleset o = null;
        try {
            o = RulesetMysqlHelper.getRulesetForId(id);
        } catch (SQLException e) {
            logger.error("error while getting ruleset with id " + id, e);
            throw new DAOException(e);
        }
        return o;
    }

    public static void saveRuleset(Ruleset ruleset) throws DAOException {
        try {
            RulesetMysqlHelper.saveRuleset(ruleset);
        } catch (SQLException e) {
            logger.error("error while saving ruleset with id " + ruleset.getId(), e);
            throw new DAOException(e);
        }
    }

    public static void deleteRuleset(Ruleset ruleset) throws DAOException {
        try {
            RulesetMysqlHelper.deleteRuleset(ruleset);
        } catch (SQLException e) {
            logger.error("error while deleting ruleset with id " + ruleset.getId(), e);
            throw new DAOException(e);
        }
    }

    public static List<Ruleset> getRulesets(String order, String filter, Integer start, Integer count) throws DAOException {
        List<Ruleset> answer = new ArrayList<Ruleset>();
        try {
            answer = RulesetMysqlHelper.getRulesets(order, filter, start, count);
        } catch (SQLException e) {
            logger.error("error while getting rulesets", e);
            throw new DAOException(e);
        }
        return answer;
    }

    public static List<Ruleset> getAllRulesets() {
        List<Ruleset> answer = new ArrayList<Ruleset>();
        try {
            answer = RulesetMysqlHelper.getAllRulesets();
        } catch (SQLException e) {
            logger.error("error while getting rulesets", e);
        }
        return answer;
    }

    @Override
    public List<? extends DatabaseObject> getList(String order, String filter, Integer start, Integer count) throws DAOException {
        return getRulesets(order, filter, start, count);
    }

    @Override
    public int getHitSize(String order, String filter) throws DAOException {
        int num = 0;
        try {
            num = RulesetMysqlHelper.getRulesetCount(order, filter);
        } catch (SQLException e) {
            logger.error("error while getting ruleset hit size", e);
            throw new DAOException(e);
        }
        return num;
    }

    /* +++++++++++++++++++++++++++++++++++++++++ Converter +++++++++++++++++++++++++++++++++++++++++++++++ */

    public static Ruleset convert(ResultSet rs) throws SQLException {
        Ruleset r = new Ruleset();
        r.setId(rs.getInt("MetadatenKonfigurationID"));
        r.setTitel(rs.getString("Titel"));
        r.setDatei(rs.getString("Datei"));
        r.setOrderMetadataByRuleset(rs.getBoolean("orderMetadataByRuleset"));
        return r;
    }

    public static ResultSetHandler<Ruleset> resultSetToRulesetHandler = new ResultSetHandler<Ruleset>() {
        @Override
        public Ruleset handle(ResultSet rs) throws SQLException {
            try {
                if (rs.next()) {
                    return convert(rs);
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
            return null;
        }

    };

    public static ResultSetHandler<List<Ruleset>> resultSetToRulesetListHandler = new ResultSetHandler<List<Ruleset>>() {
        @Override
        public List<Ruleset> handle(ResultSet rs) throws SQLException {
            List<Ruleset> answer = new ArrayList<Ruleset>();
            try {
                while (rs.next()) {
                    Ruleset o = convert(rs);
                    if (o != null) {
                        answer.add(o);
                    }
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
            return answer;
        }
    };

    @Override
    public List<Integer> getIdList(String filter) {
        return null;
    }

}
