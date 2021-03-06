/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.hadoop.shim.hdi35;

import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.hadoop.shim.HadoopConfiguration;
import org.pentaho.hadoop.shim.HadoopConfigurationFileSystemManager;
import org.pentaho.hadoop.shim.common.DriverProxyInvocationChain;
import org.pentaho.hadoop.shim.common.HadoopShimImpl;
import org.pentaho.hadoop.shim.hdi35.invocationhandler.HDIDriverInvocationHandler;

import java.sql.Driver;
import java.util.Properties;

public class HadoopShim extends HadoopShimImpl {
  @Override
  public void onLoad( HadoopConfiguration config, HadoopConfigurationFileSystemManager fsm ) throws Exception {
    registerExtraDatabaseTypes( config.getConfigProperties() );
    super.onLoad( config, fsm );
  }

  protected void registerExtraDatabaseTypes( Properties configuration ) throws KettlePluginException {
    String sparkSqlSimbaDriverName =
      configuration.getProperty( "sparksql.simba.driver", "com.simba.spark.jdbc41.Driver" );
    JDBC_POSSIBLE_DRIVER_MAP.put( "SparkSqlSimba", sparkSqlSimbaDriverName );
  }

  @Override
  public Driver getJdbcDriver( String driverType ) {
    try {
      Driver newInstance = null;
      Class<? extends Driver> clazz = JDBC_DRIVER_MAP.get( driverType );
      if ( clazz != null ) {
        newInstance = clazz.newInstance();
        return DriverProxyInvocationChain.getProxy( Driver.class, newInstance, HDIDriverInvocationHandler.class );
      } else {
        clazz = tryToLoadDriver( JDBC_POSSIBLE_DRIVER_MAP.get( driverType ) );
        if ( clazz != null ) {
          newInstance = clazz.newInstance();
          if ( driverType.equals( "Impala" ) ) {
            return DriverProxyInvocationChain.getProxy( Driver.class, newInstance, HDIDriverInvocationHandler.class );
          }
          return newInstance;
        }
        return null;
      }

    } catch ( Exception ex ) {
      throw new RuntimeException( "Unable to load JDBC driver of type: " + driverType, ex );
    }
  }
}
