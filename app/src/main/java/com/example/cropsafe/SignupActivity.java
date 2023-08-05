package com.example.cropsafe;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SignupActivity extends AppCompatActivity {

	private EditText editTextName;
	private EditText editTextEmail;
	private EditText editTextPassword;
	private EditText editTextConfirmPassword;
	private Button buttonSignUp;

	private static final String DB_URL = "jdbc:mysql://127.0.0.1:33036/cropsafe";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signup);

		// Initialize your UI elements
		editTextName = findViewById(R.id.editTextName);
		editTextEmail = findViewById(R.id.editTextEmail);
		editTextPassword = findViewById(R.id.enterPassword);
		editTextConfirmPassword = findViewById(R.id.reenterPassword);
		buttonSignUp = findViewById(R.id.buttonSignUp);

		buttonSignUp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = editTextName.getText().toString();
				String email = editTextEmail.getText().toString();
				String password = editTextPassword.getText().toString();
				String confirmPassword = editTextConfirmPassword.getText().toString();

				if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
					Toast.makeText(SignupActivity.this, "Please fill all input fields", Toast.LENGTH_SHORT).show();
				} else if (!password.equals(confirmPassword)) {
					Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
				} else if (!isValidEmail(email)) {
					Toast.makeText(SignupActivity.this, "Incorrect email format", Toast.LENGTH_SHORT).show();
				} else {
					// Perform registration process here (send data to PHP script)
					performRegistration(name, email, password);
				}
			}
		});
	}

	private boolean isValidEmail(String email) {
		return Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}

	private boolean performRegistration(String name, String email, String password) {
		// Your registration logic here

		Connection connection = null;
		PreparedStatement preparedStatement = null;

		try {
			// Establish a database connection
			connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
			Toast.makeText(SignupActivity.this, "DB connected", Toast.LENGTH_SHORT).show();
			// Check if the email already exists
			if (emailExists(connection, email)) {
				Toast.makeText(SignupActivity.this, "Email already registered", Toast.LENGTH_SHORT).show();
				return false;
			}

			// Insert user data into the database
			String insertQuery = "INSERT INTO users_reg (name, email, password) VALUES (?, ?, ?)";
			preparedStatement = connection.prepareStatement(insertQuery);
			preparedStatement.setString(1, name);
			preparedStatement.setString(2, email);
			preparedStatement.setString(3, password);


			int rowsInserted = preparedStatement.executeUpdate();
			if (rowsInserted > 0) {
				Toast.makeText(SignupActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
				return true;
			} else {
				Toast.makeText(SignupActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			// Close resources
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}


	private static boolean emailExists(Connection connection, String email) throws SQLException {
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT email FROM users WHERE email = ?";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, email);
			return preparedStatement.executeQuery().next();
		} finally {
			if (preparedStatement != null) {
				preparedStatement.close();
			}
		}
	}
}

