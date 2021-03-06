// Copyright 2012 Cloudera Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.impala.analysis;

import com.cloudera.impala.authorization.Privilege;
import com.cloudera.impala.catalog.AuthorizationException;
import com.cloudera.impala.catalog.Db;
import com.cloudera.impala.common.AnalysisException;
import com.cloudera.impala.thrift.TAccessEvent;
import com.cloudera.impala.thrift.TCatalogObjectType;
import com.cloudera.impala.thrift.TDropDbParams;

/**
 * Represents a DROP [IF EXISTS] DATABASE statement
 */
public class DropDbStmt extends StatementBase {
  private final String dbName_;
  private final boolean ifExists_;

  /**
   * Constructor for building the drop statement. If ifExists is true, an error will not
   * be thrown if the database does not exist.
   */
  public DropDbStmt(String dbName, boolean ifExists) {
    this.dbName_ = dbName;
    this.ifExists_ = ifExists;
  }

  public String getDb() { return dbName_; }
  public boolean getIfExists() { return ifExists_; }

  @Override
  public String toSql() {
    StringBuilder sb = new StringBuilder("DROP DATABASE");
    if (ifExists_) {
      sb.append(" IF EXISTS ");
    }
    sb.append(getDb());
    return sb.toString();
  }

  public TDropDbParams toThrift() {
    TDropDbParams params = new TDropDbParams();
    params.setDb(getDb());
    params.setIf_exists(getIfExists());
    return params;
  }

  @Override
  public void analyze(Analyzer analyzer) throws AnalysisException,
      AuthorizationException {
    Db db = analyzer.getCatalog().getDb(dbName_, analyzer.getUser(), Privilege.DROP);
    if (db == null && !ifExists_) {
      throw new AnalysisException(Analyzer.DB_DOES_NOT_EXIST_ERROR_MSG + dbName_);
    }
    analyzer.addAccessEvent(new TAccessEvent(dbName_,
        TCatalogObjectType.DATABASE, Privilege.DROP.toString()));

    if (analyzer.getDefaultDb().toLowerCase().equals(dbName_.toLowerCase())) {
      throw new AnalysisException("Cannot drop current default database: " + dbName_);
    }
    if (db != null && db.numFunctions() > 0) {
      throw new AnalysisException("Cannot drop non-empty database: " + dbName_);
    }
  }
}
