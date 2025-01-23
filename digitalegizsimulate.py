import requests
import time
import math

# 1. Укажите адрес вашего сервера и ID устройства
# Например, если сервер локально и порт 5055:
SERVER_URL = "http://localhost:5055"
DEVICE_ID = "12345"

# 2. Функция отправки координат по протоколу Osmand (HTTP GET)
def send_position(latitude, longitude):
    params = {
        "id": DEVICE_ID,
        "lat": latitude,
        "lon": longitude,
        "timestamp": int(time.time()),
        "hdop": 0.9,      # Опционально
        "altitude": 100,  # Опционально
        "speed": 10.0     # Условная скорость (м/с)
    }
    try:
        resp = requests.get(SERVER_URL, params=params, timeout=5)
        print(f"Отправлено: {resp.url}, статус: {resp.status_code}")
    except Exception as e:
        print(f"Ошибка при отправке данных: {e}")

# 3. Генерация списка точек от Алматы до Атырау по прямой
def generate_route(lat1, lon1, lat2, lon2, steps=100):
    """
    Генерирует `steps+1` точек, равномерно расположенных
    между (lat1, lon1) и (lat2, lon2).
    """
    lat_diff = lat2 - lat1
    lon_diff = lon2 - lon1
    route = []
    for i in range(steps + 1):
        frac = i / steps
        lat = lat1 + frac * lat_diff
        lon = lon1 + frac * lon_diff
        route.append((lat, lon))
    return route

def main():
    # Координаты (приблизительно) Алматы
    lat_almaty = 43.238293
    lon_almaty = 76.945465

    # Координаты (приблизительно) Атырау
    lat_atyrau = 47.0945
    lon_atyrau = 51.9238

    # Генерируем маршрут "туда" в 200 шагов (чем больше steps, тем больше точек и плавнее движение)
    route_forward = generate_route(lat_almaty, lon_almaty, lat_atyrau, lon_atyrau, steps=200)
    # Генерируем маршрут "обратно" (просто тот же список в обратном порядке)
    route_backward = route_forward[::-1]

    print("Старт симуляции маршрута Алматы → Атырау → обратно (по прямой)")

    # Циклично ходим туда-сюда бесконечно
    while True:
        # Идём из Алматы в Атырау
        for (lat, lon) in route_forward:
            send_position(lat, lon)
            time.sleep(5)  # Каждые 5 секунд отправляем новую точку

        # Идём из Атырау обратно в Алматы
        for (lat, lon) in route_backward:
            send_position(lat, lon)
            time.sleep(2)

if __name__ == "__main__":
    main()
