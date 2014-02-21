# Auxiliary and test functions 
import re
import os
import ast

BASE_PATH="/home/mtoth/skola/dp/hadoop-common"
LOG_FILE="/home/mtoth/skola/dp/LogFilterBase/Log-change/hadoop-rewritten-logs-short"
NEW_LOG_FILE="/home/mtoth/Desktop/hadoop-fresh-logs.txt"

# Merge OLD and NEW "output-log-file" from idea 11 to idea 12 log search export.
# Also insert new logs to appropriate position. (DOES NOT DO UPDATE!!)
def parse_log(line):
    #print "log=", line
    return line

def print_spaces(number):
    spacing = ""
    for i in range(0, number):
        spacing += " " 
    return spacing

def merge_log_files(file_old, file_new):
    fr = open(file_old, "r") 
    fnew = open(file_new, "rw")
    f_write = open("hadoop-rewritten-logs", "w")
    f_error = open("hadoop-rewritten-logs-errors", "w")

    l_number = 0    
    previous_line = None
    new_log = ""
    old_log = ""
    logs_to_change = []
    for line in fr:  
        parsed_line = ''
        space_counter = 0
        found_first_letter = False;
        for letter in line:
            if letter.isspace() and not found_first_letter:
                space_counter = space_counter + 1
            else:
                found_first_letter = True;
                parsed_line += letter #sys.stdout.write(letter)

        parsed_line = parsed_line[:-1]  # remove last \n
        
        # if previous parsed_line = None or " " call with isOld = True                       
        if space_counter == 24:            
            if previous_line.startswith('(') and not parsed_line.startswith('('):
                new_log = parse_log(parsed_line)
            else:
                old_log = parse_log(parsed_line)

            
            if old_log != "" and new_log != "":
                # print old_log + "  --  " + new_log
                logs_to_change.append( (old_log.strip(), new_log.strip()) )
                old_log = ""
                new_log = ""
            if "LOG.USING_PLUGIN_JARS(Iterables.toString(jars)" in new_log:
                break
           
        previous_line = parsed_line
        l_number = l_number + 1

    fr.close()


    # for old, new in logs_to_change:    
    #     print old, "\n", new, "\n\n"

    logs = logs_to_change
    spaces = print_spaces(28)
    i = 0
    changed = 0
    for line in fnew:
        newline = line
        line = line.lstrip()

        for old, new in logs:
            if old[old.lower().find(') '):45] in line:                
                newline = str.replace(line, line, spaces + line + spaces + new.rstrip() + "\n\n")            
                print "old=" + old, "\n", line, "new=\n", newline, changed
                changed = changed + 1
                logs_to_change.remove((old, new))                
                i = i + 1
        f_write.write(newline)
    f_write.close()

    print "Swapped= ", i, "out of", len(logs_to_change)
    f_error.write("Following logs were not matched, so they were not modified in new file. Add them manually.\n\n")
    for old, new in logs_to_change:        
        f_error.write(old+"\n")
        f_error.write(new+"\n\n")


# old input file with logs written, new output file w/o logs
#merge_log_files("/home/mtoth/skola/dp/LogFilterBase/Log-change/hadoop-all-prod-log-export.txt", "hadoop-new-logs-no-tests.txt")


# Remove test classes and logs from log-file exported from idea
def remove_test_classes(filepath):        
    lines = []
    l_number = 1             
    remove_mode = False; 
    remove_list = []

    fr = open(filepath, 'r')
    for line in fr:
        parsed_line = ""
        if line == '':
            break;
        space_counter = 0
        found_first_letter = False;
        for letter in line:
            if letter.isspace() and not found_first_letter:
                space_counter = space_counter + 1
            else:
                found_first_letter = True;
                parsed_line += letter

        parsed_line = parsed_line[:-1]  # remove last \n

        if space_counter == 4:
            if line.strip().startswith("Test"):
                remove_list.append((l_number, -1))
                print 
            print "TEST begins", line

        if space_counter == 16:
            if remove_mode:
                remove_stop_line = l_number
                remove_mode = False
                remove_list.append((remove_start_line, remove_stop_line))
            if parsed_line.lower().find("test") != -1:
                remove_mode = True
                remove_start_line = l_number

        l_number = l_number + 1
        lines.append((l_number, line))
    
    fr.close()    

    print remove_list
    print "number of lines=", len(lines)
    
    fr = open(filepath + "-no-tests", 'w')

    to_remove = []
    for removal in remove_list:
        if removal[1] == -1:
            removal = (removal[0], l_number)
        for i in range(removal[0], removal[1]):
            print i
            to_remove.append(i+1)

    for i, line in lines:
        if i not in to_remove:
            fr.write(line)  


# Move level of logs from front to back
# LOG.debug()...  -> LOG....debug();
def change_level():
    file_name = "/home/mtoth/Desktop/logy/hadoop-all-prod-log-export.txt"
    output_name = "output.log"
    f = list(enumerate(open(file_name, "rw+b")))
    out_file = open(output_name, "w")

    for i, line in f:
        line = line.rstrip()
        match = re.search('LOG\.(trace|debug|info|warn|error|fatal)\(\)', line)

        if match:       
            newline = line[:match.start()+3] + line[match.end():-1] + line[match.start()+3:match.end()] + ";"
            print line
            print 'newline=', newline
            line = newline
        out_file.write(line+"\n")

# Search all packages/modules in BASE_DIR
def search_all_modules():
    fr = open(LOG_FILE, 'r')
    
    l_number = 0    
    previous_line = None
    packages = []

    for line in fr:
        parsed_line = ""        
        if line == '':
            break;
        space_counter = 0
        found_first_letter = False;
        for letter in line:
            if letter.isspace() and not found_first_letter:
                space_counter = space_counter + 1
            else:
                found_first_letter = True;
                parsed_line += letter #sys.stdout.write(letter)

        parsed_line = parsed_line[:-1]  # remove last \n

        if space_counter == 12:
            package = parse_package(parsed_line)
            #print 'pckg=', package
            packages.append(package)

    #print packages
    for name, path in packages:  
        if path == None: 
            print "package=", name, path
        elif "common" in path:
            print name, path

def search_my_logs():
    pattern = re.compile(r'^LOG\.[A-Z_]+\(*')
    for line in open(LOG_FILE, "r"):
        line =  line.strip()
        match = pattern.match(line)
        if match:
            print match.group()

def findnth(haystack, nth, splitter):
    parts = haystack.split(splitter, nth+1)
    return len(haystack)-len(parts[-1])-1


def regexp_search():
    l = [
     '"dsadasd" + test, sad + "dsad"',
     '"Can\'t Failed to load a state.", e, asd, " blabolasd."',
     '"Unknown child node with the name: " + childNodeName',  # !!!!!!  string + string
     '"User " + user + " removed from activeUsers, currently: " + var2',
     '"update:" + " application=" + applicationId + " request=" + request',
     '"Test" + test.id() + " test 2=" + max.id().getStuff() +',
     'max.getId(), error',
     'blabol, bla2',
     '"ffd"',
     '"comment 1 +"',
     '"  classpath = " + System.getProperty("java.class.path")',
     '"  args = " + Arrays.asList(args),"  version = " + VersionInfo.getVersion(),\
            "  classpath = " + System.getProperty("java.class.path"),\
            "  build = " + VersionInfo.getUrl("asd") + " -r "',
]
    # MEGALOG
    ''' 
    "Initializing " + queueName + "\n" +
        "capacity = " + capacity +
        " [= (float) configuredCapacity / 100 ]" + "\n" + 
        "asboluteCapacity = " + absoluteCapacity +
        " [= parentAbsoluteCapacity * capacity ]" + "\n" +
        "maxCapacity = " + maximumCapacity +
        " [= configuredMaxCapacity ]" + "\n" +
        "absoluteMaxCapacity = " + absoluteMaxCapacity +
        " [= 1.0 maximumCapacity undefined, " +
        "(parentAbsoluteMaxCapacity * maximumCapacity) / 100 otherwise ]" + 
        "\n" +
        "userLimit = " + userLimit +
        " [= configuredUserLimit ]" + "\n" +
        "userLimitFactor = " + userLimitFactor +
        " [= configuredUserLimitFactor ]" + "\n" +
        "maxApplications = " + maxApplications +
        " [= configuredMaximumSystemApplicationsPerQueue or" + 
        " (int)(configuredMaximumSystemApplications * absoluteCapacity)]" + 
        "\n" +
        "maxApplicationsPerUser = " + maxApplicationsPerUser +
        " [= (int)(maxApplications * (userLimit / 100.0f) * " +
        "userLimitFactor) ]" + "\n" +
        "maxActiveApplications = " + maxActiveApplications +
        " [= max(" + 
        "(int)ceil((clusterResourceMemory / minimumAllocation) * " + 
        "maxAMResourcePerQueuePercent * absoluteMaxCapacity)," + 
        "1) ]" + "\n" +
        "maxActiveAppsUsingAbsCap = " + maxActiveAppsUsingAbsCap +
        " [= max(" + 
        "(int)ceil((clusterResourceMemory / minimumAllocation) *" + 
        "maxAMResourcePercent * absoluteCapacity)," + 
        "1) ]" + "\n" +
        "maxActiveApplicationsPerUser = " + maxActiveApplicationsPerUser +
        " [= max(" +
        "(int)(maxActiveApplications * (userLimit / 100.0f) * " +
        "userLimitFactor)," +
        "1) ]" + "\n" +
        "usedCapacity = " + usedCapacity +
        " [= usedResourcesMemory / " +
        "(clusterResourceMemory * absoluteCapacity)]" + "\n" +
        "absoluteUsedCapacity = " + absoluteUsedCapacity +
        " [= usedResourcesMemory / clusterResourceMemory]" + "\n" +
        "maxAMResourcePerQueuePercent = " + maxAMResourcePerQueuePercent +
        " [= configuredMaximumAMResourcePercent ]" + "\n" +
        "minimumAllocationFactor = " + minimumAllocationFactor +
        " [= (float)(maximumAllocationMemory - minimumAllocationMemory) / " +
        "maximumAllocationMemory ]" + "\n" +
        "numContainers = " + numContainers +
        " [= currentNumContainers ]" + "\n" +
        "state = " + state +
        " [= configuredState ]" + "\n" +
        "acls = " + aclsString +
        " [= configuredAcls ]" + "\n" + 
        "nodeLocalityDelay = " +  nodeLocalityDelay + "\n"'''
    
    banned_words = ["THE", "A", "AN"]
    METHOD_NAME_SEPARATORS = 4

    for s in l:
        #           match.any("string.property")                     string(don't want)         variable(s)     string(dont want)
        pattern = r'(?P<var2>[\w]+[\.[\w]+]?\(\"[\w]+(?:\.[\w]+)*\"\))|'
        pattern = r'(?:\"[^"]+\")*(?P<var>[^"]+)(?:\"[^"]+\")*|(?:\"[^"]+\")'            
        #found =  re.findall(pattern, s, re.IGNORECASE)



        comment_pattern = r'\"([^"]+)\"'#(?:[^"]+)(\"[^"]+\")+'
        found =  re.findall(comment_pattern, s, re.IGNORECASE)
        messages = []        

        for f in found:       
            f = re.sub("'", "", f)   
            f = re.sub(r'[\W]+(?!\')', " ", f.strip().upper())      
            f = re.sub(" ", "_", f.strip())
            messages.append(f)
        
        # print found, "--->", messages  


        if messages != []:
            out = ""
            for m in messages:                
                message_list = m.split("_")
                # remove short or banned words from method names
                for x in message_list:                    
                    if len(x) <= 1:
                        continue
                    if x in banned_words:
                        continue
                    out = out + x + "_"                

                                        
            if out.endswith("_"):
                out = out[:-1]
                if out.count("_")-1 >= METHOD_NAME_SEPARATORS:                    
                    out = out[:findnth(out, METHOD_NAME_SEPARATORS, "_")]
            print "out=", out


        #variables = []
        #if found != ['']:   
        #    print found     
        #     for found_var in found: 
        #         if str(found_var).strip() != '':
        #             if "+" in found_var:
        #                 found_var = found_var.replace("+", "").strip()

        #             if "," in found_var:
        #                 variables = [v.strip() for v in found_var.split(",") if v.strip() != '']

        #             elif found_var != '':                    
        #                 variables.append(found_var.strip())
                   
        # print found, "---->", variables

def is_my_log(line):
    # "LOG.SOMETHING_INTERESTING("
    pattern = re.compile(r'^LOG\.[A-Z_]+\(*')
    return pattern.match(line.strip())


def fetch_full_log():

    '''
    LOG.info("Giving handle (fileId:" + handle.getFileId()
        + ") to client for export " + path);        
    }
    '''
    #line = '(115: 5) LOG.info("Giving handle (fileId:" + handle.getFileId()'
    PATH='/home/mtoth/skola/dp/hadoop-common/hadoop-hdfs-project/hadoop-hdfs-nfs/src/main/java/org/apache/hadoop/hdfs/nfs/mount/RpcProgramMountd.java'
    POS=[115, 5]
    OLD='LOG.info("Giving handle (fileId:" + handle.getFileId()'
    NEW='LOG.LOG(path, e).tag("org.apache.hadoop.hdfs.nfs").error();' # it;s previous log!!

    f = open(PATH, "r+b")
    try:
        java_file = list(enumerate(f))        
        log = ""       

        for i, line in java_file[POS[0]-1:]:
            line = line.strip()
            log = log + line
            if ";" in line:
                # if line is not COMMENT - break! else ignore line
                if "//" not in line:
                    POS.append(i+1)
                    break

        print "log = ", log, POS
    except:
        print 'Error while reading file!', PATH
        raise
    finally:
        f.close()
        return log



def test():
    l = ["asda", "dsadsa", "test()"]
    generated_log = "(" + ', '.join(map(str, l)) + ').tag("' 
    #print generated_log
    #print str(l)

    slovnik2 = {}
    slovnik = {"a" : ["asd", "ad"], "b" : ["sdf"],  "c" : ["dfg"]}

    print slovnik.keys()
    z = "blabol"
    for x in slovnik.keys():
        print "key="+x
        if z not in slovnik[x]:
            print "\t", slovnik[x]
            slovnik[x].append(z)
        # for i in slovnik[x]:
        #     print "\t" + i

    print slovnik.items()



    for x in slovnik.keys():
        for val in slovnik[x]:
            print "va=", val


def ast_tree(filepath):
    f = open(filepath, "r")
    output =  '''
LOG.info("Giving handle (fileId:" + handle.getFileId()
    + ") to client for export " + path);        
    '''
    
    print output
    print ast.dump(ast.parse(output))

#regexp_search()


JAVA_FILE = "/home/mtoth/skola/dp/hadoop-common/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/VersionInfo.java"
JAVA_FILE = "/home/mtoth/Desktop/fds"
#ast_tree(JAVA_FILE)


l = [
 '"dsadasd" + test, sad + "dsad"',
 '"Failed to load state.", e, asd, " blabolasd."',
 '"Unknown child node with name: " + childNodeName',  # !!!!!!  string + string
 '"User " + user + " removed from activeUsers, currently: " + var2',
 '"update:" + " application=" + applicationId + " request=" + request',
 '"Test" + test.id() + " test 2=" + max.id().getStuff() +',
 'max.getId(), error',
 'blabol, bla2',
 '"ffd"',
 '"comment 1 +"',
 '"  classpath = " + System.getProperty("java.class.path")',
]

def finder(line):
    # mask the strings
    st = '' 
    m = ''
    for i, c in enumerate(line):
        if c in ['"', "'"]:
            st += c
        m += c if len(st) == 0 else '~'
        st = '' if len(st) == 2 else st

    #split = m.split(',') if ',' in m else m.split('+')
    split = [spl2 for spl1 in m.split('+') for spl2 in spl1.split(',')]
    pos = 0
    for spl in split:
        if spl.replace('~', '').strip() != '':        
            out = line[pos : pos+len(spl)].strip()
            yield out
        pos += len(spl) + 1


def test():
    bla = ''' * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.nfs.mount;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.mount.MountEntry;
import org.apache.hadoop.mount.MountInterface;
import org.apache.hadoop.mount.MountResponse;
import org.apache.hadoop.nfs.nfs3.FileHandle;
import org.apache.hadoop.nfs.nfs3.Nfs3Status;
import org.apache.hadoop.oncrpc.RpcAcceptedReply;
import org.apache.hadoop.oncrpc.RpcCall;
import org.apache.hadoop.oncrpc.RpcProgram;
import org.apache.hadoop.oncrpc.XDR;
import org.jboss.netty.channel.Channel;

/**
 * RPC program corresponding to mountd daemon. See {@link Mountd}.
 */
public class RpcProgramMountd extends RpcProgram implements MountInterface {
  private static final Log LOG = LogFactory.getLog(RpcProgramMountd.class);
  public static final int PROGRAM = 100005;
  public static final int VERSION_1 = 1;
  public static final int VERSION_2 = 2;
  public static final int VERSION_3 = 3;
  public static final int PORT = 4242;

  public XDR dump(XDR out, int xid, InetAddress client) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("MOUNT NULLOP : " + " client: " + client);
    }

    List<MountEntry> copy = new ArrayList<MountEntry>(mounts);
    MountResponse.writeMountList(out, xid, copy);
    return out;
  }

  public XDR umnt(XDR xdr, XDR out, int xid, InetAddress client) {
    String path = xdr.readString();
    if (LOG.isDebugEnabled()) {
      LOG.debug("MOUNT UMNT path: " + path + " client: " + client);
    }
  }
'''



    # match = re.search(r'import [\w\.]+log[\w\.]+;', bla.lower())
    # print "x"
    # if match:
    #     print match.group()

    match = re.search(r'log\.is[\w]+enabled', bla.lower())
    if match:
        log_global = match.group().replace('log', 'LogGlobal')
        print log_global
    # pattern = re.compile(r"\w+version")
    # pattern = re.compile(r"import\w*")
    # print bla.lower()

    # match = pattern.match(bla.lower())
    # if match:
    #     print match.group()
    # print re.findall(r'*log=*', bla.lower())
    

# test()


def generate_log_global():
    FILE = """package cz.muni.fi.logger;\n
public static class LogGlobal {\n
%s
}"""
    METHOD = '''\tpublic static Boolean is%sEnabled() {
\t\treturn True;
\t}\n\n'''
    
    level_list = ['Debug', 'Info', 'Error', 'Fatal']
    method = ""

    for i in [METHOD % x for x in level_list]:
        method += i
    full_file =  FILE % method.rstrip()
     
    print full_file
       

generate_log_global()