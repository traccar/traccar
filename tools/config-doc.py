#!/usr/bin/env python3

import re
import os
import argparse

_KEYS_FILE = os.path.join(
    os.path.dirname(__file__), "../src/main/java/org/traccar/config/Keys.java"
)


def get_config_keys():
    """Parses Keys.java to extract keys to be used in configuration files

    Args: None

    Returns:
        list: A list of dict containing the following keys -
            'key': A dot separated name of the config key
            'description': A list of str
    """
    types_match_re = re.compile(r"List\.of\(([^)]+)\)", re.DOTALL)
    keys = []

    with open(_KEYS_FILE, "r") as f:
        config = re.findall(r"/\*\*\s.*?\);", f.read(), re.DOTALL)
        for i in config:
            lines = i.splitlines()
            index = -1
            default = None
            if "List.of" not in lines[index]:
                default = lines[index].strip()[:-2]
                index -= 1
            types_match = types_match_re.search(lines[index])
            types = map(lambda x: x[8:].lower(), types_match[1].split(", "))
            index -= 1
            key = lines[index].strip()[1:-2]
            key = "[protocol]" + key if key.startswith('.') else key
            description = " ".join([l.strip()[2:] for l in lines if l.startswith("     * ")])
            keys.append(
                {
                    "key": key,
                    "description": description,
                    "types": types,
                    "default": default,
                }
            )

    return keys


def get_html():
    return ("\n").join(
        [
            f"""        <div class="card mt-3">
          <div class="card-body">
              <h5 class="card-title">
                {x["key"]}
              </h5>
              <p class="card-text">
                {x["description"]}
              </p>
          </div>
        </div>"""
            for x in get_config_keys()
        ]
    )


def get_pug():
    return ("\n").join(
        [
            f"""  div(class='card mt-3')
    div(class='card-body')
      h5(class='card-title') {x["key"]} {" ".join(map("#[span(class='badge badge-dark') {:}]".format, x["types"]))}
      p(class='card-text') {x["description"]}{f"\n      p(class='card-text') Default value: {x["default"]}" if x["default"] is not None else ""}"""
            for x in get_config_keys()
        ]
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Parses Keys.java to extract keys to be used in configuration files"
    )
    parser.add_argument(
        "--format", choices=["pug", "html"], default="pug", help="default: 'pug'"
    )
    args = parser.parse_args()

    def get_output():
        if args.format == 'html':
            return get_html()
        
        return get_pug()

    print(get_output())
