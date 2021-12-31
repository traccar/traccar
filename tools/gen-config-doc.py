#!/usr/bin/python

import re


def get_config_key_descriptions():
    desc_re = re.compile(r"(/\*\*\n|\s+\*/|\s+\*)")
    key_match_re = re.compile(r"\(\n(.+)\);", re.DOTALL)
    key_split_re = re.compile(r",\s+", re.DOTALL)
    snippets = []

    with open("../src/main/java/org/traccar/config/Keys.java", "r") as f:
        code = f.read()
        config = re.findall(
            r"(/\*\*.*?\*/)\n\s+(public static final Config.*?;)", code, re.DOTALL
        )
        for i in config:
            try:
                key_match = key_match_re.search(i[1])
                if key_match:
                    description = desc_re.sub("", i[0]).strip()
                    terms = [x.strip() for x in key_split_re.split(key_match.group(1))]
                    key = terms[0].replace('"', "")
                    default = terms[2] if len(terms) == 3 else None
                    snippets.append(
                        f"""        <div class="card mt-3">
          <div class="card-body">
              <h5 class="card-title">
                {key} <span class="badge badge-dark">config</span>
              </h5>
              <p class="card-text">
                {description}{f" Default: {default}." if default else ""}
              </p>
          </div>
        </div>"""
                    )
            except IndexError:
                # will continue if key_match.group(1) or terms[0] does not exist
                # for some reason
                pass

    return ("\n").join(snippets)


if __name__ == "__main__":
    html = get_config_key_descriptions()
    print(html)
