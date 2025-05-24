#!/usr/bin/env python3
"""
Pruebas de estrés para User Service usando Locust
Simula casos de uso reales del sistema de e-commerce
"""

from locust import HttpUser, task, between
import json
import random
import string

class UserServiceStressTest(HttpUser):
    """
    Clase para pruebas de estrés del microservicio User Service
    """
    wait_time = between(1, 5)  # Tiempo de espera entre requests (1-5 segundos)
    
    def on_start(self):
        """Se ejecuta al inicio de cada usuario simulado"""
        self.user_data = {
            "firstName": f"TestUser{random.randint(1000, 9999)}",
            "lastName": f"Lastname{random.randint(1000, 9999)}",
            "email": f"test{random.randint(1000, 9999)}@example.com",
            "phone": f"555{random.randint(1000000, 9999999)}",
            "imageUrl": "http://example.com/avatar.jpg",
            "credential": {
                "username": f"user{random.randint(1000, 9999)}",
                "password": "password123",
                "roleBasedAuthority": "ROLE_USER",
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True
            }
        }
        
    @task(3)
    def get_all_users(self):
        """
        Prueba de estrés: Obtener todos los usuarios
        Peso: 3 (se ejecuta 3 veces más frecuentemente)
        """
        with self.client.get("/api/users", 
                           headers={"Content-Type": "application/json"},
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed to get users: {response.status_code}")
    
    @task(2)
    def get_user_by_id(self):
        """
        Prueba de estrés: Obtener usuario por ID
        Peso: 2
        """
        user_id = random.randint(1, 100)  # Simula IDs existentes
        with self.client.get(f"/api/users/{user_id}", 
                           headers={"Content-Type": "application/json"},
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                # 404 es esperado para usuarios que no existen
                response.success()
            else:
                response.failure(f"Unexpected status code: {response.status_code}")
    
    @task(1)
    def create_user(self):
        """
        Prueba de estrés: Crear nuevo usuario
        Peso: 1 (menos frecuente para no sobrecargar la base de datos)
        """
        # Generar datos únicos para cada usuario
        unique_id = random.randint(10000, 99999)
        user_data = self.user_data.copy()
        user_data["firstName"] = f"StressUser{unique_id}"
        user_data["email"] = f"stress{unique_id}@example.com"
        user_data["credential"]["username"] = f"stressuser{unique_id}"
        
        with self.client.post("/api/users",
                            json=user_data,
                            headers={"Content-Type": "application/json"},
                            catch_response=True) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed to create user: {response.status_code}")
    
    @task(2)
    def get_user_by_username(self):
        """
        Prueba de estrés: Buscar usuario por username
        Peso: 2
        """
        # Lista de usernames que podrían existir
        usernames = ["selimhorri", "amineladjimi", "omarderouiche", "admin", 
                    f"user{random.randint(1, 100)}"]
        username = random.choice(usernames)
        
        with self.client.get(f"/api/users/username/{username}",
                           headers={"Content-Type": "application/json"},
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                # 404 es esperado para usuarios que no existen
                response.success()
            else:
                response.failure(f"Failed to get user by username: {response.status_code}")

class CredentialServiceStressTest(HttpUser):
    """
    Clase para pruebas de estrés del servicio de Credenciales
    """
    wait_time = between(1, 3)
    
    @task(3)
    def get_all_credentials(self):
        """Prueba de estrés: Obtener todas las credenciales"""
        with self.client.get("/api/credentials", 
                           headers={"Content-Type": "application/json"},
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed to get credentials: {response.status_code}")
    
    @task(2)
    def get_credential_by_username(self):
        """Prueba de estrés: Buscar credencial por username"""
        usernames = ["selimhorri", "amineladjimi", "omarderouiche", "admin"]
        username = random.choice(usernames)
        
        with self.client.get(f"/api/credentials/username/{username}",
                           headers={"Content-Type": "application/json"},
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                response.success()
            else:
                response.failure(f"Failed to get credential by username: {response.status_code}")

class UserServiceSpikeTest(HttpUser):
    """
    Prueba de picos de tráfico - simula cargas extremas repentinas
    """
    wait_time = between(0.1, 0.5)  # Muy poco tiempo de espera para generar picos
    
    @task
    def rapid_user_requests(self):
        """Genera múltiples requests rápidos para simular picos de tráfico"""
        endpoints = [
            "/api/users",
            "/api/users/1",
            "/api/users/username/admin",
            "/api/credentials"
        ]
        
        endpoint = random.choice(endpoints)
        with self.client.get(endpoint,
                           headers={"Content-Type": "application/json"},
                           catch_response=True) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Spike test failed: {response.status_code}")

# Configuración para diferentes escenarios de prueba
if __name__ == "__main__":
    """
    Para ejecutar las pruebas:
    
    1. Prueba básica (10 usuarios concurrentes):
       locust -f user_service_stress_test.py --host=http://localhost:8080
    
    2. Prueba de estrés (100 usuarios):
       locust -f user_service_stress_test.py --host=http://localhost:8080 -u 100 -r 10
    
    3. Prueba de picos (500 usuarios rápidos):
       locust -f user_service_stress_test.py UserServiceSpikeTest --host=http://localhost:8080 -u 500 -r 50
    """
    print("Configurar las pruebas de estrés para User Service")
    print("Host por defecto: http://localhost:8080")
    print("Ejecutar: locust -f user_service_stress_test.py --host=http://localhost:8080") 