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

import static com.milaboratory.mixcr.assembler.ReadToCloneMapping.MappingType.Dropped;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NavigableSet;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mixcr.assembler.ReadToCloneMapping;
import com.milaboratory.mixcr.basictypes.Clone;
import com.milaboratory.mixcr.basictypes.SequencePartitioning;
import com.milaboratory.mixcr.basictypes.VDJCAlignments;
import com.milaboratory.mixcr.basictypes.VDJCObject;
import com.milaboratory.mixcr.cli.ActionAssemble;
import com.milaboratory.mixcr.reference.GeneFeature;
import com.milaboratory.mixcr.reference.ReferencePoint;

public abstract class AbstractFieldExtractors {
    protected static final String NULL = "";
    protected static final DecimalFormat SCORE_FORMAT = new DecimalFormat("#.#");

    static Field[] descriptors = null;

    public abstract Field[] getFields();

    public FieldExtractor parse(OutputMode outputMode, Class clazz, String[] args) {
        for (Field field : getFields())
            if (field.canExtractFrom(clazz) && args[0].equalsIgnoreCase(field.getCommand()))
                return field.create(outputMode, Arrays.copyOfRange(args, 1, args.length));
        throw new IllegalArgumentException("Not a valid options: " + Arrays.toString(args));
    }

    public ArrayList<String>[] getDescription(Class clazz) {
        ArrayList<String>[] description = new ArrayList[]{new ArrayList(), new ArrayList()};
        for (Field field : getFields())
            if (field.canExtractFrom(clazz)) {
                description[0].add(field.getCommand());
                description[1].add(field.getDescription());
            }

        return description;
    }

    /* Some typedefs */
    static abstract class PL_O extends FieldParameterless<VDJCObject> {
        PL_O(String command, String description, String hHeader, String sHeader) {
            super(VDJCObject.class, command, description, hHeader, sHeader);
        }
    }

    static abstract class PL_A extends FieldParameterless<VDJCAlignments> {
        PL_A(String command, String description, String hHeader, String sHeader) {
            super(VDJCAlignments.class, command, description, hHeader, sHeader);
        }
    }

    static abstract class PL_C extends FieldParameterless<Clone> {
        PL_C(String command, String description, String hHeader, String sHeader) {
            super(Clone.class, command, description, hHeader, sHeader);
        }
    }

    static abstract class WP_O<P> extends FieldWithParameters<VDJCObject, P> {
        protected WP_O(String command, String description) {
            super(VDJCObject.class, command, description);
        }
    }

    protected static abstract class FeatureExtractorDescriptor extends WP_O<GeneFeature> {
        final String hPrefix, sPrefix;

        protected FeatureExtractorDescriptor(String command, String description, String hPrefix, String sPrefix) {
            super(command, description);
            this.hPrefix = hPrefix;
            this.sPrefix = sPrefix;
        }

        @Override
        protected GeneFeature getParameters(String[] string) {
            if (string.length != 1)
                throw new RuntimeException("Wrong number of parameters for " + getCommand());
            return GeneFeature.parse(string[0]);
        }

        @Override
        protected String getHeader(OutputMode outputMode, GeneFeature parameters) {
            return choose(outputMode, hPrefix + " ", sPrefix) + GeneFeature.encode(parameters);
        }

        @Override
        protected String extractValue(VDJCObject object, GeneFeature parameters) {
            NSequenceWithQuality feature = object.getFeature(parameters);
            if (feature == null)
                return NULL;
            return convert(feature);
        }

        public abstract String convert(NSequenceWithQuality seq);
    }

    protected abstract class AbstractExtractSequence extends FieldParameterless<VDJCObject> {

        protected AbstractExtractSequence(Class targetType, String command, String description, String hHeader,
                String sHeader) {
            super(targetType, command, description, hHeader, sHeader);
        }
        
    }

    protected static class ExtractSequenceQuality extends FieldParameterless<VDJCObject> {
        protected ExtractSequenceQuality(Class targetType, String command, String description, String hHeader, String sHeader) {
            super(targetType, command, description, hHeader, sHeader);
        }

        @Override
        protected String extract(VDJCObject object) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; ; i++) {
                sb.append(object.getTarget(i).getQuality());
                if (i == object.numberOfTargets() - 1)
                    break;
                sb.append(",");
            }
            return sb.toString();
        }
    }

    protected static class ExtractReferencePointPosition extends WP_O<ReferencePoint> {
        protected ExtractReferencePointPosition() {
            super("-positionOf",
                    "Exports position of specified reference point inside target sequences " +
                            "(clonal sequence / read sequence).");
        }

        @Override
        protected ReferencePoint getParameters(String[] string) {
            if (string.length != 1)
                throw new RuntimeException("Wrong number of parameters for " + getCommand());
            return ReferencePoint.parse(string[0]);
        }

        @Override
        protected String getHeader(OutputMode outputMode, ReferencePoint parameters) {
            return choose(outputMode, "Position of ", "positionOf") +
                    ReferencePoint.encode(parameters, true);
        }

        @Override
        protected String extractValue(VDJCObject object, ReferencePoint parameters) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; ; i++) {
                sb.append(object.getPartitionedTarget(i).getPartitioning().getPosition(parameters));
                if (i == object.numberOfTargets() - 1)
                    break;
                sb.append(",");
            }
            return sb.toString();
        }
    }

    protected abstract class AbstractExtractDefaultReferencePointsPositions extends PL_O {
        public AbstractExtractDefaultReferencePointsPositions() {
            super("-defaultAnchorPoints", "Outputs a list of default reference points (like CDR2Begin, FR4End, etc. " +
                    "see documentation for the full list and formatting)", "Ref. points", "refPoints");
        }
        
        public AbstractExtractDefaultReferencePointsPositions(String command, String description, String hHeader, String sHeader) {
            super(command, description, hHeader, sHeader);
        }

        @Override
        protected String extract(VDJCObject object) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; ; i++) {
                SequencePartitioning partitioning = object.getPartitionedTarget(i).getPartitioning();
                for (int j = 0; ; j++) {
                    int referencePointPosition = partitioning.getPosition(ReferencePoint.DefaultReferencePoints[j]);
                    if (referencePointPosition >= 0)
                        sb.append(referencePointPosition);
                    if (j == ReferencePoint.DefaultReferencePoints.length - 1)
                        break;
                    sb.append(":");
                }
                if (i == object.numberOfTargets() - 1)
                    break;
                sb.append(",");
            }
            return sb.toString();
        }

    }


    protected static AbstractField<VDJCAlignments> alignmentsToClone(
            final String command, final String description, final boolean printMapping) {
        return new AbstractField<VDJCAlignments>(VDJCAlignments.class, command, description) {
            @Override
            public FieldExtractor<VDJCAlignments> create(OutputMode outputMode, String[] args) {
                return new AlignmentToCloneExtractor(outputMode, args[0], printMapping);
            }
        };
    }

    protected static final class AlignmentToCloneExtractor
            implements FieldExtractor<VDJCAlignments>, Closeable {
        private final OutputMode outputMode;
        private final DB db;
        private final NavigableSet<ReadToCloneMapping> byAls;
        private final boolean printMapping;
        private final Iterator<ReadToCloneMapping> mappingIterator;
        private ReadToCloneMapping currentMapping = null;

        public AlignmentToCloneExtractor(OutputMode outputMode, String file, boolean printMapping) {
            this.outputMode = outputMode;
            this.printMapping = printMapping;
            this.db = DBMaker.newFileDB(new File(file))
                    .transactionDisable()
                    .make();
            this.byAls = db.getTreeSet(ActionAssemble.MAPDB_SORTED_BY_ALIGNMENT);
            this.mappingIterator = byAls.iterator();
        }

        @Override
        public String getHeader() {
            if (printMapping)
                return choose(outputMode, "Clone mapping", "cloneMapping");
            else
                return choose(outputMode, "Clone Id", "cloneId");
        }

        @Override
        public String extractValue(VDJCAlignments object) {
            if (currentMapping == null && mappingIterator.hasNext())
                currentMapping = mappingIterator.next();
            if (currentMapping == null)
                return NULL;

            while (currentMapping.getAlignmentsId() < object.getAlignmentsIndex() && mappingIterator.hasNext())
                currentMapping = mappingIterator.next();
            if (currentMapping.getAlignmentsId() != object.getAlignmentsIndex())
                return printMapping ? Dropped.toString().toLowerCase() : NULL;

            int cloneIndex = currentMapping.getCloneIndex();
            ReadToCloneMapping.MappingType mt = currentMapping.getMappingType();
            if (mt == Dropped)
                return printMapping ? mt.toString().toLowerCase() : NULL;
            return printMapping ? Integer.toString(cloneIndex) + ":" + mt.toString().toLowerCase() : Integer.toString(cloneIndex);
        }

        @Override
        public void close() throws IOException {
            db.close();
        }
    }

    protected static final class CloneToReadsExtractor
            implements FieldExtractor<Clone>, Closeable {
        private final OutputMode outputMode;
        private final DB db;
        private final NavigableSet<ReadToCloneMapping> byClones;
        private final Iterator<ReadToCloneMapping> mappingIterator;
        private ReadToCloneMapping currentMapping;

        public CloneToReadsExtractor(OutputMode outputMode, String file) {
            this.outputMode = outputMode;
            this.db = DBMaker.newFileDB(new File(file))
                    .transactionDisable()
                    .make();
            this.byClones = db.getTreeSet(ActionAssemble.MAPDB_SORTED_BY_CLONE);
            this.mappingIterator = byClones.iterator();
        }

        @Override
        public String getHeader() {
            return choose(outputMode, "Reads", "reads");
        }

        @Override
        public String extractValue(Clone clone) {
            if (currentMapping == null && mappingIterator.hasNext())
                currentMapping = mappingIterator.next();
            if (currentMapping == null)
                return NULL;

            while (currentMapping.getCloneIndex() < clone.getId() && mappingIterator.hasNext())
                currentMapping = mappingIterator.next();

            long count = 0;
            StringBuilder sb = new StringBuilder();
            while (currentMapping.getCloneIndex() == clone.getId()) {
                ++count;
                assert currentMapping.getCloneIndex() == currentMapping.getCloneIndex();
                sb.append(currentMapping.getReadId()).append(",");
                if (!mappingIterator.hasNext())
                    break;
                currentMapping = mappingIterator.next();
            }
            //count == object.getCount() only if addReadsCountOnClustering=true
            assert count >= clone.getCount() : "Actual count: " + clone.getCount() + ", in mapping: " + count;
            if (sb.length() != 0)
                sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        @Override
        public void close() throws IOException {
            db.close();
        }
    }

    protected static String choose(OutputMode outputMode, String hString, String sString) {
        switch (outputMode) {
            case HumanFriendly:
                return hString;
            case ScriptingFriendly:
                return sString;
            default:
                throw new NullPointerException();
        }
    }
}
