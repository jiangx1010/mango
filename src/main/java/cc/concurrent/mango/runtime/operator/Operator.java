/*
 * Copyright 2014 mango.concurrent.cc
 *
 * The Mango Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package cc.concurrent.mango.runtime.operator;


import cc.concurrent.mango.runtime.DataSourceFactoryHolder;

/**
 * db操作接口
 *
 * @author ash
 */
public interface Operator {

    public void setDataSourceFactoryHolder(DataSourceFactoryHolder dataSourceFactoryHolder);

    public void setStatsCounter(StatsCounter statsCounter);

    public Object execute(Object[] methodArgs);

}
