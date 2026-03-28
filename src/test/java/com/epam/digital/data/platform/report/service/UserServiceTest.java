/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.report.service;

import static com.epam.digital.data.platform.report.service.UserService.CREATE_ROLE;
import static com.epam.digital.data.platform.report.service.UserService.DELETE_ROLE;
import static com.epam.digital.data.platform.report.service.UserService.UPDATE_ROLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.exception.DatabaseUserException;
import com.epam.digital.data.platform.report.model.Role;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String TEST_PASSWORD = UUID.randomUUID().toString();

  @Mock
  DataSource dataSource;

  @Mock
  Connection connection;

  @Mock
  CallableStatement statement;

  @Mock
  ResultSet resultSet;

  UserService instance;

  @BeforeEach
  void init() throws SQLException {
    instance = new UserService(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    lenient().when(connection.prepareCall(any())).thenReturn(statement);
  }

  @Test
  void shouldReturnTrueOnCheckUserExistenceIfResultSetNotEmpty() throws SQLException {
    when(connection.prepareStatement(UserService.CHECK_USER_EXISTENCE)).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);

    var actual = instance.isUserExists("officer");

    assertThat(actual).isTrue();
    verify(connection).prepareStatement(UserService.CHECK_USER_EXISTENCE);
    verify(statement).setString(1, "analytics_officer");
    verify(statement).executeQuery();
  }

  @Test
  void shouldReturnFalseOnCheckUserExistenceIfResultSetEmpty() throws SQLException {
    when(connection.prepareStatement(UserService.CHECK_USER_EXISTENCE)).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    var actual = instance.isUserExists("officer");

    assertThat(actual).isFalse();
    verify(connection).prepareStatement(UserService.CHECK_USER_EXISTENCE);
    verify(statement).setString(1, "analytics_officer");
    verify(statement).executeQuery();
  }

  @Test
  void shouldCreateUser() throws SQLException {
    var role = new Role();
    role.setName("officer");
    role.setPassword(TEST_PASSWORD);

    instance.createUser(role);

    verify(connection).prepareCall(CREATE_ROLE);
    verify(statement).setString(1, "\"analytics_officer\"");
    verify(statement).setString(2, TEST_PASSWORD);
    verify(statement).execute();
  }

  @Test
  void shouldUpdateUser() throws SQLException {
    var role = new Role();
    role.setName("officer");
    role.setPassword(TEST_PASSWORD);

    instance.updateUser(role);

    verify(connection).prepareCall(UPDATE_ROLE);
    verify(statement).setString(1, "\"analytics_officer\"");
    verify(statement).setString(2, TEST_PASSWORD);
    verify(statement).execute();
  }

  @Test
  void shouldThrowDatabaseUserExceptionWhenSQLExceptionDuringCreate() throws SQLException {
    when(statement.execute()).thenThrow(new SQLException());

    var role = new Role();
    role.setName("officer");
    role.setPassword(TEST_PASSWORD);

    assertThatThrownBy(() -> instance.createUser(role))
        .isInstanceOf(DatabaseUserException.class)
        .hasMessageContaining("Could not create user");
  }

  @Test
  void shouldDropUser() throws SQLException {
    instance.deleteUser("officer");

    verify(connection).prepareCall(DELETE_ROLE);
    verify(statement).setString(1, "\"analytics_officer\"");
    verify(statement).execute();
  }

  @Test
  void shouldThrowDatabaseUserExceptionWhenSQLExceptionDuringDrop() throws SQLException {
    when(statement.execute()).thenThrow(new SQLException());

    assertThatThrownBy(() -> instance.deleteUser("officer"))
        .isInstanceOf(DatabaseUserException.class)
        .hasMessageContaining("Could not drop user");
  }
}
