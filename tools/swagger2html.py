#!/usr/bin/python

import sys, argparse, json, re

def handleException(etype, e=None):
    if etype == 'KeyError':
        print "Error: Required property {} not found".format(e)
    elif etype == 'IOError':
        print "Error ({}): {}".format(e.errno, e.strerror)
    elif etype == 'ValueError':
        print "Error: Unable to parse input as JSON"
    elif etype == 'Custom':
        print e
    sys.exit(1)

def get_json(filename):
    try:
        with open(filename) as json_file:
            json_data = json.load(json_file)
            return json_data
    except IOError as e:
        handleException('IOError', e)
    except ValueError:
        handleException('ValueError')
    except:
        print "Unexpected error: {}".format(sys.exc_info()[0])
        raise

def write_file(filename, body):
    try:
        with open(filename, 'w') as md_file:
            md_file.write(body)
    except IOError as e:
        handleException('IOError', e)

def make_header(json_data):
    try:
        if not 'swagger' in json_data:
            raise KeyError
        info = json_data['info']
        md = "<h1>{}</h1>\n".format(info['title'])
        md += "<p>Version: {}</p>\n".format(info['version'])
        if 'license' in info:
            md += "<p>License: "
            license = info['license']
            if 'url' in license:
                md += '<a href="{}">{}</a>'.format(license['url'], license['name'])
            else:
                md += license['name']
            md += '</p>\n'
        if 'contact' in info:
            contact = info['contact']
            if 'name' in contact or 'email' in contact:
                md += '<p>Contact: '
                if not 'name' in contact:
                    md += '<a href="mailto:{0}">{0}</a>'.format(contact['email'])
                elif not 'email' in contact:
                    md += contact['name']
                else:
                    md += '{0} &lt;<a href="mailto:{1}"&gt;{1}</a>'.format(contact['name'], contact['email'])
                md += '  \n'
            if 'url' in contact:
                md += "<p>Website: {}</p>\n".format(contact['url'])
        if 'termsOfService' in info:
            md += '<p>Terms of Service: {}</p>\n'.format(info['termsOfService'])
        if 'host' in json_data:
            md += '<p>Base URL: '
            base = json_data['host']
            if 'basePath' in json_data:
                base += json_data['basePath']
            else:
                base += '/'
            if 'schemes' in json_data:
                md += (', ').join(map(
                    lambda x: '<a href="{0}://{1}">{0}://{1}</a>'.format(x, base),
                    json_data['schemes']
                ))
            else:
                md += '<a href="{0}">{0}</a>'.format(base)
            md += '</p>\n'
        if 'description' in info:
            md += '<p>Description: {}</p>\n'.format(info['description'])
        md += '\n'
        return md
    except KeyErrori as e:
        handleException('KeyError', e)

def make_ref(ref):
    href = ref.split('/')[1:]
    return '<a href="#{}">{}</a>'.format('_'.join(href), href[-1])

def get_ref(ref, raw):
    keys = ref.split('/')
    return raw[keys[1]][keys[2]]

def make_html(s):
    reg = re.compile(r"[*_]{3}(.+?)[*_]{3}")
    s = reg.sub(r"<strong><em>\1</em></strong>", s)
    reg = re.compile(r"[*_]{2}(.+?)[*_]{2}")
    s = reg.sub(r"<strong>\1</strong>", s)
    reg = re.compile(r"[*_](.+?)[*_]")
    s = reg.sub(r"<em>\1</em>", s)
    reg = re.compile(r"\`(.+?)\`")
    s = reg.sub(r"<code>\1</code>", s)
    return s

def make_table(data):
    md = '<table class="table-bordered">\n'
    md += '  <thead>\n'
    md += '    <tr>\n'
    for col in data[0]:
        md += '      <td>{}</td>\n'.format(col)
    md += '    </tr>\n'
    md += '  </thead>\n'
    md += '  <tbody>\n'
    for row in data[1:]:
        md += '    <tr>\n'
        for cell in row:
            md += '      <td>{}</td>\n'.format(cell)
        md += '    </tr>\n'
    md += '  </tbody>\n'
    md += '</table>\n'
    return md

def make_params_table(itemsraw, raw):
    items = []
    for item in itemsraw:
        if '$ref' in item:
            items.append(get_ref(item['$ref'], raw))
        else:
            items.append(item)
    try:
        fields = list(set([]).union(*map(lambda x: x.keys(), items)))
        row = [ 'Name', 'ParamType' ]
        if 'description' in fields:
            row.append('Description')
        if 'required' in fields:
            row.append('Required')
        if 'type' in fields:
            row.append('DataType')
        if 'schema' in fields:
            row.append('Schema')
        table = [ row ]
        for item in items:
            row = [ "<em>{}</em>".format(item['name']), item['in'] ]
            if 'description' in fields:
                if 'description' in item:
                    row.append(make_html(item['description']))
                else:
                    row.append('')
            if 'required' in fields:
                required = 'False'
                if 'required' in item and item['required']:
                    required = "<strong>True</strong>"
                row.append(required)
            if 'type' in fields:
                type = ''
                if 'type' in item:
                    if item['type'] == 'array':
                        type = "[ <em>{}</em> ]".format(item['items']['type'])
                    else:
                        type = item['type']
                        if 'format' in item:
                            type += " ({})".format(item['format'])
                        type = "<em>{}</em>".format(type)
                row.append(type)
            if 'schema' in fields:
                if 'schema' in item:
                    if '$ref' in item['schema']:
                        row.append(make_ref(item['schema']['$ref']))
                else:
                    row.append('')
            table.append(row)
        return make_table(table)
    except KeyError as e:
        handleException('KeyError', e)

def make_responses_table(responses):
    try:
        fields = list(
            set([]).union(*map(lambda x: x.keys(),
                map(lambda x: responses[x], responses.keys())
            ))
        )
        row = [ 'Status Code', 'Description' ]
        if 'headers' in fields:
            row.append('Headers')
        if 'schema' in fields:
            row.append('Schema')
        if 'examples' in fields:
            row.append('Examples')
        table = [ row ]
        for key in sorted(responses):
            response = responses[key]
            row = [ "<em>{}</em>".format(key), make_html(response['description']) ]
            if 'headers' in fields:
                header = ''
                if 'headers' in response:
                    hrow = []
                    for header, h_obj in response['headers'].iteritems():
                        hrow += "{} ({})".format(header, h_obj['type'])
                        if 'description' in h_obj:
                            hrow += ": {}".format(h_obj['description'])
                    header = '  \n'.join(hrow)
                row.append(header)
            if 'schema' in fields:
                schema = ''
                if 'schema' in response:
                    if '$ref' in response['schema']:
                        schema += make_ref(response['schema']['$ref'])
                    if 'type' in response['schema']:
                        if response['schema']['type'] == 'array':
                            if '$ref' in response['schema']['items']:
                                schema += make_ref(response['schema']['items']['$ref'])
                            schema = "[ {} ]".format(schema)
                row.append(schema)
            if 'examples' in fields:
                if 'examples' in response:
                    row.append(response['examples'])
                else:
                    row.append('')
            table.append(row)
        return make_table(table)
    except KeyError as e:
        handleException('KeyError', e)

def sorted_by_method(section):
    sorting_function = lambda x: [ 'GET', 'POST', 'PUT', 'DELETE' ].index(
        x['title'].split(' ')[0]
    )
    return sorted(sorted(section), key=sorting_function)

def make_paths(sections, json_data):
    md = '<h2><a name="paths"></a>Paths</h2>\n'
    for key in sorted(sections):
        md += '<h3><a name="paths_{0}"></a>{0}</h3>\n'.format(key)
        for section in sorted_by_method(sections[key]):
            md += '<h4><a name="{}"></a><code>{}</code></h4>\n'.format(
                section['href'], section['title']
            )
            operation = section['operation']
            if 'summary' in operation:
                md += '<p>Summary: {}</p>\n'.format(make_html(operation['summary']))
            if 'description' in operation:
                md += '<p>Description: {}</p>\n'.format(make_html(operation['description']))
            md += '<h5>Parameters</h5>\n'
            if 'parameters' in operation and len(operation['parameters']) > 0:
                md += make_params_table(operation['parameters'], json_data)
            else:
                md += "<p><em>None</em></p>\n"
            md += '<h5>Responses</h5>\n'
            md += make_responses_table(operation['responses'])
        md += '\n'
    md += '\n'
    return md

def make_contents(path_section, json_data):
    md = '<h3>Contents</h3>\n'
    md += '<ul>\n'
    md += '  <li><a href="#paths">Paths</a>\n'
    md += '    <ul>\n'
    for key in sorted(path_section):
        md += '      <li><a href="#paths_{0}">{0}</a>\n'.format(key)
        md += '        <ul>\n'
        for section in sorted_by_method(path_section[key]):
            md += '          <li><a href="#{}">{}</a></li>\n'.format(
                section['href'], section['title']
            )
        md += '        </ul>\n'
        md += '      </li>\n'
    md += '    </ul>\n'
    md += '  </li>\n'
    md += '  <li><a href="#definitions">Models</a>\n'
    md += '    <ul>\n'
    for key in sorted(json_data['definitions']):
        md += '      <li><a href="#definitions_{0}">{0}</a></li>\n'.format(key)
    md += '    </ul>\n'
    md += '  </li>\n'
    md += '</ul>\n'
    return md

def make_definitions(json_data):
    md = '<h2><a name="definitions"></a>Models</h2>\n'
    for name in sorted(json_data['definitions']):
        md += '<h3><a name="definitions_{0}"></a>{0}</h3>\n'.format(name)
        model = json_data['definitions'][name]
        if 'properties' in model:
            fields = list(
                set(['type']).union(
                    *map(lambda x: x.keys(),
                        map(lambda x: model['properties'][x], model['properties'].keys())
                    )
                )
            )
            row = [ 'Property', 'Type' ]
            if 'description' in fields:
                row.append('Description')
            table = [ row ]
            for key in sorted(model['properties']):
                property = model['properties'][key]
                row = [ "<em>{}</em>".format(key) ]
                if 'type' in property:
                    type = property['type']
                    if 'format' in property:
                        type += " ({})".format(property['format'])
                    row.append("<em>{}</em>".format(type))
                elif '$ref' in property:
                    row.append(make_ref(property['$ref']))
                else:
                    row.append('')
                if 'description' in fields:
                    if 'description' in property:
                        row.append(make_html(property['description']))
                    else:
                        row.append('')
                table.append(row)
        md += make_table(table)
    return md

def make_markdown(json_data):
    path_sections = {}
    for endpoint in json_data['paths']:
        path_split = endpoint.split('/')
        path_key = path_split[1]
        if not path_key in path_sections:
            path_sections[path_key] = []
        for method, operation in json_data['paths'][endpoint].iteritems():
            if 'operationId' in operation:
                link = operation['operationId']
            else:
                link = ''.join([
                    c for c in endpoint if c not in ['/', '{', '}']
                ])
            path_sections[path_key].append({
                'title': '{} {}'.format(method.upper(), endpoint),
                'href': 'paths_{}_{}'.format(link, method.upper()),
                'operation': operation
            })
    md = make_header(json_data)
    md += make_contents(path_sections, json_data)
    md += make_paths(path_sections, json_data)
    md += make_definitions(json_data)
    return md

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('SPECFILE', help="path to swagger.json file")
    parser.add_argument('OUTFILE', help="path to output HTML file")
    args = parser.parse_args()

    marked_down = make_markdown(get_json(args.SPECFILE))

    if args.OUTFILE:
        write_file(args.OUTFILE, marked_down)
        print " success: {}".format(args.OUTFILE)
    else:
        print marked_down

if __name__ == '__main__':
    main()
