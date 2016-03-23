/*
 * Copyright (c) 2014-2015, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
 * (here and after addressed as Inventors)
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact the Inventors using one of the following
 * email addresses: chudakovdm@mail.ru, chudakovdm@gmail.com
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.mixcr.export;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.milaboratory.mixcr.basictypes.Clone;
import com.milaboratory.mixcr.basictypes.VDJCHit;
import com.milaboratory.mixcr.reference.GeneType;
import com.milaboratory.mixcr.reference.Locus;
import com.milaboratory.mixcr.util.VersionInfoProvider;
import com.milaboratory.util.VersionInfo;

public final class VidjilInfoWriter<T> extends AbstractInfoWriter<T> {
	boolean clonesPut = false;
	StringBuilder sb = new StringBuilder();

	public VidjilInfoWriter(String file) throws FileNotFoundException {
		super(file);
	}


	private void initialize() {
        if (!initialized) {
           sb.append('{');
           sb.append('\n');
            initialized = true;
        }
    }

    @Override
    public void put(T t) {
        initialize();
        if(!clonesPut) {
            sb.append("\"clones\": [\n");
            clonesPut = true;
        }
    	String s;
        sb.append("{");
        for (int i = 0; i < fieldExtractors.size(); ++i) {
            s = fieldExtractors.get(i).extractValue(t);
            if (s.length() > 1) {
                sb.append(s.substring(1,  s.length()-1));
            } else {
                sb.append(s);
            }
            if (i == fieldExtractors.size() - 1)
                break;
            sb.append(",\n");
        }
        sb.append("\n},\n");
    }

    @Override
    public void putReads(List<Clone> clones) {
        initialize();
        Map<String, Long> locusCount = new HashMap<>();
        VDJCHit h = null;
        long total = 0;
        for(Clone clone : clones) {
            for(GeneType g : GeneType.values()) {
                h = null;
                h = clone.getBestHit(g);
                if(h != null) {
                    break;
                }
            }
            String locus = h.getAllele().getGeneGroup().getLocus().getId();
            Long count = clone.getCount();
            total += count;
            if(locusCount.containsKey(locus)) {
                count += locusCount.get(locus);
            }
            locusCount.put(locus, count);
        }
        sb.append("\"reads\": {\n");
        sb.append("\"germline\": {\n");
        String prefix = "";
        for(Locus l : Locus.values()) {
            sb.append(prefix);
            prefix = ",\n";
            sb.append("\"" + l.getId() + "\": [\n");
            if(locusCount.containsKey(l.getId())) {
                sb.append(locusCount.get(l.getId()));
            } else {
                sb.append("0");
            }
            sb.append("\n]");
        }
        sb.append("\n},\n");
        // TODO calculate segmented
        sb.append("\"segmented\": [\n" + total + "\n],\n");
        sb.append("\"total\": [\n" + total + "\n]\n},\n");

    }

    @Override
    public void close() throws IOException {
        initialize();
        String s = sb.toString();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (!clonesPut) {
            outputStream.write(s.getBytes());
            outputStream.write("\"clones\": [],\n".getBytes());
        } else {
            outputStream.write(s.substring(0, s.length()-2).getBytes());
            outputStream.write("],\n".getBytes());
        }
        String date = df.format(new Date());
        outputStream.write("\"germlines\": {\"custom\": {\"3\": [],\"4\": [],\"5\": [],\"shortcut\": \"X\"}},\n".getBytes());
        outputStream.write("\"samples\": {\n".getBytes());
        outputStream.write(("\"timestamp\": [\"" + date +"\"],\n").getBytes());
        outputStream.write("\"commandline\": [\"\"],\n".getBytes());
        outputStream.write("\"log\": [\"\"],\n".getBytes());
        outputStream.write("\"number\": 1,\n".getBytes());
        outputStream.write("\"original_names\": [\"\"],\n".getBytes());
        outputStream.write(("\"producer\": [\"" + VersionInfoProvider.getVersionString(VersionInfoProvider.OutputType.ToFile) + "\"],\n").getBytes());
        outputStream.write(("\"run_timestamp\": [\"" + date + "\"]\n").getBytes());
        outputStream.write("},\n".getBytes());
        outputStream.write("\"similarity\": [[]],\n".getBytes());
        outputStream.write("\"vidjil_json_version\": \"2014.10\"".getBytes());

        outputStream.write('\n');
        outputStream.write('}');
        outputStream.close();
        for (FieldExtractor<? super T> fe : fieldExtractors)
            if (fe instanceof Closeable)
                ((Closeable) fe).close();
    }
}
