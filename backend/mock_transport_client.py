#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
模拟冷藏车GPS和温度数据上报的测试脚本
用法: python mock_transport_client.py
"""

import time
import random
import json
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080/api"

VEHICLES = [
    {"plateNumber": "京A·88888", "lat_base": 39.9042, "lng_base": 116.4074, "temp_normal": (2.0, 8.0)},
    {"plateNumber": "沪B·66666", "lat_base": 31.2304, "lng_base": 121.4737, "temp_normal": (2.0, 8.0)},
    {"plateNumber": "粤C·12345", "lat_base": 23.1291, "lng_base": 113.2644, "temp_normal": (2.0, 8.0)},
    {"plateNumber": "川D·99999", "lat_base": 30.5728, "lng_base": 104.0668, "temp_normal": (2.0, 8.0)},
    {"plateNumber": "鲁E·77777", "lat_base": 36.6512, "lng_base": 117.1201, "temp_normal": (2.0, 8.0)},
]

# 记录每辆车最近的位置
vehicle_state = {}


def report_data(plate_number, temperature, lat, lng):
    url = f"{BASE_URL}/transport/report"
    data = json.dumps({
        "plateNumber": plate_number,
        "temperature": round(temperature, 2),
        "latitude": round(lat, 6),
        "longitude": round(lng, 6),
        "locationAddress": f"模拟位置-{plate_number}"
    }).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            status = "✓" if result.get("success") else "✗"
            print(f"[{status}] {plate_number} 温度={temperature:.2f}°C 位置=({lat:.4f},{lng:.4f}) {result.get('message','')}")
            return result.get("success", False)
    except urllib.error.URLError as e:
        print(f"[✗] {plate_number} 上报失败: {e}")
        return False


def generate_position(vehicle):
    plate = vehicle["plateNumber"]
    if plate not in vehicle_state:
        vehicle_state[plate] = {
            "lat": vehicle["lat_base"],
            "lng": vehicle["lng_base"],
            "stuck_temp_count": 0,
            "last_temp": None
        }
    state = vehicle_state[plate]

    # 模拟车辆缓慢移动
    state["lat"] += random.uniform(-0.0005, 0.0008)
    state["lng"] += random.uniform(-0.0005, 0.0008)

    # 30% 概率让某辆车模拟传感器卡死（温度不变）
    if plate == "沪B·66666" and random.random() < 0.7:
        if state["last_temp"] is None:
            state["last_temp"] = round(random.uniform(*vehicle["temp_normal"]), 2)
        temp = state["last_temp"]
        state["stuck_temp_count"] += 1
    else:
        temp = random.uniform(*vehicle["temp_normal"])
        state["last_temp"] = None
        state["stuck_temp_count"] = 0

    return state["lat"], state["lng"], temp


def main():
    print("=" * 60)
    print("  疫苗冷链运输模拟客户端 - 每10秒上报一次数据")
    print("  目标服务:", BASE_URL)
    print("  提示: 车辆 沪B·66666 会模拟传感器卡死，用于测试预警功能")
    print("=" * 60)

    try:
        while True:
            print(f"\n--- {time.strftime('%Y-%m-%d %H:%M:%S')} ---")
            for v in VEHICLES:
                lat, lng, temp = generate_position(v)
                report_data(v["plateNumber"], temp, lat, lng)
                time.sleep(0.3)
            time.sleep(10)
    except KeyboardInterrupt:
        print("\n已停止")


if __name__ == "__main__":
    main()
