#!/usr/bin/python

import re
import os


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
    desc_re = re.compile(r"(/\*\*\n|\s+\*/|\s+\*)")
    key_match_re = re.compile(r"\(\n(.+)\);", re.DOTALL)
    key_split_re = re.compile(r",\s+", re.DOTALL)
    keys = []

    with open(_KEYS_FILE, "r") as f:
        config = re.findall(
            r"(/\*\*.*?\*/)\n\s+(public static final Config.*?;)", f.read(), re.DOTALL
        )
        for i in config:
            try:
                key_match = key_match_re.search(i[1])
                if key_match:
                    terms = [x.strip() for x in key_split_re.split(key_match.group(1))]
                    key = terms[0].replace('"', "")
                    description = [
                        x.strip().replace("\n", "")
                        for x in desc_re.sub("\n", i[0]).strip().split("\n\n")
                    ]
                    if len(terms) == 3:
                        description.append(f"Default: {terms[2]}")
                    keys.append(
                        {
                            "key": key,
                            "description": description,
                        }
                    )
            except IndexError:
                # will continue if key_match.group(1) or terms[0] does not exist
                # for some reason
                pass

    return keys


def get_html():
    return ("\n").join(
        [
            f"""        <div class="card mt-3">
          <div class="card-body">
              <h5 class="card-title">
                {x["key"]} <span class="badge badge-dark">config</span>
              </h5>
              <p class="card-text">
                {"<br /> ".join(x["description"])}
              </p>
          </div>
        </div>"""
            for x in get_config_keys()
        ]
    )


if __name__ == "__main__":
    html = get_html()
    print(html)
