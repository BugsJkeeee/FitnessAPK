#!/usr/bin/env python3
"""
Собирает контент приложения из tools/content/*.py:
  app/src/main/assets/exercises.json, workouts.json, фото упражнений в assets/exercises/<id>/
  docs/База_тренировок.md — перечень для просмотра.

Фото берутся из локального клона https://github.com/yuhonas/free-exercise-db (общественное достояние):
  python3 tools/build_content.py /путь/к/free-exercise-db
"""
import json
import math
import os
import shutil
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..")
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "content"))
from exercises import EXERCISES  # noqa: E402
from workouts import CARDIO, FUNCTIONAL, STRENGTH  # noqa: E402

MUSCLES = {"chest": "Грудь", "back": "Спина", "legs": "Ноги", "shoulders": "Плечи", "arms": "Руки", "abs": "Пресс", "full": "Всё тело"}
EQUIPMENT = {
    "bodyweight": "Собственный вес", "barbell": "Штанга", "dumbbell": "Гантели", "kettlebell": "Гиря",
    "pullup_bar": "Турник", "dip_bars": "Брусья", "box": "Ящик", "rower": "Гребной тренажёр",
    "machine": "Тренажёры и блоки", "bench": "Скамья",
    "treadmill": "Беговая дорожка", "bike": "Велотренажёр", "elliptical": "Эллипс", "stepper": "Степпер",
}
FORMATS = {"FOR_TIME": "For Time", "AMRAP": "AMRAP", "EMOM": "EMOM", "TABATA": "Табата", "INTERVALS": "Интервалы", "STEADY": "Непрерывно", "SETS": "Подходы"}


def build(source_db):
    assets = os.path.join(ROOT, "app", "src", "main", "assets")
    images_dir = os.path.join(assets, "exercises")
    shutil.rmtree(images_dir, ignore_errors=True)
    os.makedirs(images_dir)

    exercises, by_id = [], {}
    for ex_id, name, muscles, equipment, image_src, technique in EXERCISES:
        assert ex_id not in by_id, ex_id
        assert all(m in MUSCLES for m in muscles), ex_id
        assert all(e in EQUIPMENT for e in equipment), ex_id
        images = []
        if image_src:
            src = os.path.join(source_db, "exercises", image_src)
            assert os.path.isdir(src), image_src
            os.makedirs(os.path.join(images_dir, ex_id))
            for i in (0, 1):
                path = os.path.join(src, f"{i}.jpg")
                if os.path.exists(path):
                    shutil.copy(path, os.path.join(images_dir, ex_id, f"{i}.jpg"))
                    images.append(f"exercises/{ex_id}/{i}.jpg")
        item = dict(id=ex_id, name=name, muscles=muscles, equipment=equipment, images=images, technique=technique)
        exercises.append(item)
        by_id[ex_id] = item

    workouts = []

    def add(prefix, n, w, cardio=False):
        items = w["items"]
        ex_ids = [i[0] for i in items]
        for e in ex_ids:
            assert e in by_id, (w["name"], e)
        equipment = sorted({e for x in ex_ids for e in by_id[x]["equipment"]} - {"bodyweight"})
        muscles = []
        for x in ex_ids:
            for m in by_id[x]["muscles"]:
                if m not in muscles:
                    muscles.append(m)
        fmt, t = w["format"], w["timer"]
        if fmt == "SETS":
            kind = "strength"
            total_sets = sum(i[1] for i in items)
            duration = int(math.ceil(total_sets * 2.5 / 5.0) * 5)
            items_json = [dict(exercise=i[0], sets=i[1], reps=i[2]) for i in items]
            timer = None
        else:
            kind = "cardio" if cardio else "functional"
            if kind == "functional" and "full" not in muscles:
                muscles.append("full")
            items_json = [dict(exercise=i[0], dose=i[1]) for i in items]
            if fmt == "FOR_TIME":
                duration, timer = t["cap"], dict(mode="FOR_TIME", capSec=t["cap"] * 60)
            elif fmt == "AMRAP":
                duration, timer = t["minutes"], dict(mode="AMRAP", durationSec=t["minutes"] * 60)
            elif fmt == "STEADY":
                duration, timer = t["minutes"], dict(mode="COUNTDOWN", durationSec=t["minutes"] * 60)
            elif fmt == "EMOM":
                duration = int(math.ceil(t["interval"] * t["rounds"] / 60))
                timer = dict(mode="EMOM", intervalSec=t["interval"], rounds=t["rounds"])
            else:
                duration = int(math.ceil((t["work"] + t["rest"]) * t["rounds"] / 60))
                timer = dict(mode="INTERVALS", workSec=t["work"], restSec=t["rest"], rounds=t["rounds"])
        workouts.append(dict(
            id=f"{prefix}{n:03d}", name=w["name"], type=kind, format=fmt, level=w["level"], durationMin=duration,
            muscles=muscles, equipment=equipment, items=items_json, timer=timer, description=w["desc"],
        ))

    for n, w in enumerate(FUNCTIONAL, 1):
        add("f", n, w)
    for n, w in enumerate(STRENGTH, 1):
        add("s", n, w)
    for n, w in enumerate(CARDIO, 1):
        add("c", n, w, cardio=True)
    names = [w["name"] for w in workouts]
    assert len(names) == len(set(names)), "повторяются названия"

    with open(os.path.join(assets, "exercises.json"), "w", encoding="utf-8") as f:
        json.dump(exercises, f, ensure_ascii=False, indent=1)
    with open(os.path.join(assets, "workouts.json"), "w", encoding="utf-8") as f:
        json.dump(workouts, f, ensure_ascii=False, indent=1)
    write_doc(workouts, by_id)
    print(f"упражнений: {len(exercises)} (с фото: {sum(1 for e in exercises if e['images'])}), тренировок: {len(workouts)}")


def write_doc(workouts, by_id):
    level = {1: "лёгкая", 2: "средняя", 3: "тяжёлая"}
    lines = ["# База тренировок Tempo", "",
             "Перечень стартовой базы. Правки — списком в чат: что убрать, что поменять, что добавить.", ""]
    for kind, title in (("functional", "Функциональные комплексы"), ("strength", "Силовые тренировки"), ("cardio", "Кардио")):
        group = [w for w in workouts if w["type"] == kind]
        lines += [f"## {title} ({len(group)})", ""]
        for w in group:
            eq = ", ".join(EQUIPMENT[e] for e in w["equipment"]) or "без оборудования"
            lines.append(f"**{w['name']}** — {FORMATS[w['format']]}, ~{w['durationMin']} мин, {level[w['level']]}; {eq}")
            for i in w["items"]:
                name = by_id[i["exercise"]]["name"]
                dose = f"{i['sets']}×{i['reps']}" if "sets" in i else i["dose"]
                lines.append(f"- {name}" + (f" — {dose}" if dose else ""))
            if w["description"]:
                lines.append(f"\n_{w['description']}_")
            lines.append("")
    with open(os.path.join(ROOT, "docs", "База_тренировок.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


if __name__ == "__main__":
    build(sys.argv[1] if len(sys.argv) > 1 else "/home/user/yuhonas/free-exercise-db")
